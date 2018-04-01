package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.config.ServiceConfig
import io.paradoxical.aetr.core.db.dao.tables._
import io.paradoxical.aetr.core.model._
import io.paradoxical.rdb.slick.providers.SlickDBProvider
import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StepDb @Inject()(
  clock: Clock,
  provider: SlickDBProvider,
  dataMappers: DataMappers,
  config: ServiceConfig,
  steps: Steps,
  runs: Runs,
  children: StepChildren,
  composer: StepTreeComposer,
  runDaoManager: RunDaoManager
)(implicit executionContext: ExecutionContext) {

  import dataMappers._
  import provider.driver.api._

  def getStep(stepTreeId: StepTreeId): Future[StepTree] = {
    val idQuery = sql"""
                     WITH RECURSIVE getChild(kids) AS (
                       SELECT ${stepTreeId}
                       UNION ALL
                       SELECT child_id FROM step_children
                       JOIN getChild ON kids = step_children.id
                     )
                     SELECT * FROM getChild""".as[StepTreeId]

    val nodesQuery = for {
      ids <- idQuery
      treeNodes <- steps.query.filter(_.id inSet ids).result
      treeChildren <- children.query.filter(_.id inSet ids).result
    } yield {
      (treeChildren, treeNodes)
    }

    provider.withDB(nodesQuery.withPinnedSession).map {
      case (children, nodes) =>
        val allSteps = composer.reconstitute(nodes, children)

        allSteps.find(_.id == stepTreeId).get
    }
  }

  def deleteStep(stepTree: StepTree): Future[Unit] = {
    val decomposer = new StepTreeDecomposer(stepTree)

    val deleteExistingChildren = children.query.filter(_.id inSet decomposer.dao.map(_.id)).delete

    val deleteTrees = children.query.filter(_.id inSet decomposer.dao.map(_.id)).delete

    provider.withDB {
      DBIO.seq(
        deleteExistingChildren,
        deleteTrees
      ).transactionally
    }
  }

  def upsertStep(stepTree: StepTree): Future[Unit] = {
    val decomposer = new StepTreeDecomposer(stepTree)

    val insertDaos = DBIO.sequence(decomposer.dao.map(steps.query.insertOrUpdate))

    val deleteExistingChildren = children.query.filter(_.id inSet decomposer.dao.map(_.id)).delete

    val reInsertNewChildren = children.query.forceInsertAll(decomposer.children)

    provider.withDB {
      DBIO.seq(
        insertDaos,
        deleteExistingChildren,
        reInsertNewChildren
      ).transactionally
    }
  }

  def upsertRun(run: Run): Future[Unit] = {
    val daos = runDaoManager.runToDao(run)

    val updateDao = DBIO.sequence(daos.map(upsertIfVersion))

    provider.withDB {
      updateDao.transactionally
    }.map(_ => {})
  }

  def getRun(rootId: RootId): Future[Run] = {
    val relatedToRoot = runs.query.filter(_.root === RunInstanceId(rootId.value)).result

    provider.withDB(relatedToRoot).flatMap(resolveRunFromTreeNodes(rootId, _))
  }

  /**
   * Given a query to find the related run items for a tree,
   * execute the query and resolve the final run tree
   *
   * @param rootId
   * @return
   */
  private def resolveRunFromTreeNodes(rootId: RootId, data: Seq[RunDao]): Future[Run] = {
    val root = data.find(_.id.value == rootId.value).get

    getStep(root.stepTreeId).map(tree => {
      runDaoManager.reconstitute(rootId, data, tree)
    })
  }

  def setRunState(
    id: RunInstanceId,
    version: Version,
    state: RunState,
    result: Option[ResultData]
  ): Future[Boolean] = {
    val now = Instant.now(clock)

    val update = runs.updateWhere(
      r => r.id === id && r.version === version,
      run => (run.version, run.state, run.result, run.lastUpdatedAt, run.stateUpdatedAt),
      (version.inc(), state, result, now, now)
    )

    provider.withDB(update).map(updated => updated == 1)
  }

  def findRuns(state: RunState): Future[List[RootId]] = {
    val pendingRoots = runs.query.filter(r => r.state === state && (r.id === r.root)).result

    provider.withDB(pendingRoots).map(roots => {
      roots.map(r => RootId(r.id.value)).toList
    })
  }

  /**
   * Locks all nodes in a tree for update
   * executes the block, then returns a result
   *
   * @param rootId
   * @param block
   * @tparam T
   * @return
   */
  def lock[T](rootId: RootId)(block: Run => T): Future[Option[T]] = {
    val now = Instant.now(clock)

    val newLockId = LockId(UUID.randomUUID())

    val lockExpirationTime = now.plus(config.dbLockTime.toSeconds, ChronoUnit.SECONDS)

    // if nobody's ever locked it,
    // or the lock is expired (it was set to expire and it is currently later than that)
    // then acquire a lock
    val lockQuery = runs.query.
      filter(r =>
        r.root === rootId.asRunInstance &&
        r.actionLockedTill.isEmpty || r.actionLockedTill <= now
      ).
      map(r => (r.actionLockedTill, r.lockId)).
      update((Some(lockExpirationTime), Some(newLockId)))

    // unlock the instance and the lock key is the
    // time we expected to lock till
    val unlockQuery = runs.query.
      filter(r =>
        r.root === rootId.asRunInstance &&
        r.lockId === newLockId
      ).
      map(r => (r.actionLockedTill, r.lockId)).
      update((None, None))

    provider.withDB(lockQuery).flatMap(results => {
      // if the lock was acquired, resolve the tree
      // and allow someone to do work with it
      // and attempt a safe unlock
      if (results > 0) {
        for {
          data <- getRun(rootId).map(block).map(Some(_))
          _ <- provider.withDB(unlockQuery)
        } yield {
          data
        }
      } else {
        Future.successful(None)
      }
    })
  }

  private def upsertIfVersion(dao: RunDao): DBIO[Int] = {
    for {
      existing <- runs.query.filter(_.id === dao.id).result.headOption
      next = dao.copy(version = dao.version.inc())
      result <- if (existing.isDefined) {
        runs.query.filter(r => r.id === next.id && r.version === dao.version).update(next)
      } else {
        (runs.query += next) andThen DBIO.successful(1)
      }
    } yield {
      if (result == 0) {
        throw VersionMismatchError()
      }

      result
    }
  }
}

case class VersionMismatchError() extends RuntimeException()