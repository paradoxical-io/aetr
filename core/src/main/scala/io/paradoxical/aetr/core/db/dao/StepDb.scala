package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.config.ServiceConfig
import io.paradoxical.aetr.core.db.dao.tables._
import io.paradoxical.aetr.core.graph.RunManager
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
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  import dataMappers._
  import provider.driver.api._

  def getRootSteps(): Future[Seq[StepTree]] = {
    val rootsQ = steps.query.filter(s => s.id === s.root || s.root.isEmpty)

    provider.withDB {
      for {
        rootItems <- rootsQ.result
      } yield {
        rootItems
      }
    }.map {
      case (nodes) =>
        composer.reconstitute(nodes, Nil)
    }
  }

  def getSteps(ids: List[StepTreeId]): Future[List[StepTree]] = {
    // TODO: Optimize to one query
    Future.sequence(ids.map(getStep))
  }

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

  def upsertRun(run: Run, input: Option[ResultData] = None): Future[RunInstanceId] = {
    val daos = runDaoManager.runToDao(run.copy(input = input))

    val updateDao = DBIO.sequence(daos.map(upsertIfVersion))

    provider.withDB {
      updateDao.transactionally
    }.map(_ => run.id)
  }

  def getRunTree(rootId: RootId): Future[Run] = {
    val relatedToRoot = runs.query.filter(_.root === RunInstanceId(rootId.value)).result

    provider.withDB(relatedToRoot).flatMap(resolveRunFromTreeNodes(rootId, _))
  }

  def getRun(runInstanceId: RunInstanceId): Future[RunDao] = {
    val run = runs.query.filter(_.id === runInstanceId).result.head

    provider.withDB(run)
  }

  /**
   * Finds only runs related to actions in the state
   *
   * @param states
   * @param rootsOnly If true, only roots of trees are returned
   *                  otherwise only leaves of trees
   * @return
   */
  def findRuns(states: List[RunState], rootsOnly: Boolean = false): Future[Seq[StepRunDao]] = {
    val rootsInState =
      runs.query.
        join(steps.query).
        on { case (r, s) => r.stepTreeId === s.id }.
        filter { case (r, s) => r.state inSet states }.
        map { case (r, s) => (s, r) }

    val query =
      if (rootsOnly) {
        rootsInState.filter { case (s, r) => r.id === r.root }
      } else {
        rootsInState.filter { case (s, r) => s.execution.isDefined }
      }

    provider.withDB(query.result).map(r => r.map(StepRunDao.tupled))
  }

  def findRelatedRuns(stepTreeId: StepTreeId): Future[Seq[RunDao]] = {
    // TODO: only show roots?
    provider.withDB {
      runs.query.filter(_.stepTreeId === stepTreeId).result
    }
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
      new RunManager(runDaoManager.reconstitute(rootId, data, tree)).root
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
      run => (run.version, run.state, run.output, run.lastUpdatedAt),
      (version.inc(), state, result, now)
    )

    provider.withDB(update).map(updated => updated == 1)
  }

  def findUnlockedRuns(state: RunState): Future[List[RootId]] = {
    val pendingRoots = runs.query.filter(r =>
      r.state === state &&
      r.id === r.root &&
      (r.actionLockedTill.isEmpty || r.actionLockedTill <= Instant.now(clock))
    ).result

    provider.withDB(pendingRoots).map(roots => {
      roots.map(r => RootId(r.id.value)).toList
    })
  }

  /**
   * Locks all nodes in a tree via an optimistic lock on the tree nodes
   * if the lock is acquired, the action is run, then the lock is released
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

    // NOTE: Only locking on the root id, since you can't lock on sub nodes anyways
    // this means that the remaining tree will look "unlocked" from a db perspective
    // but it also means we dont have to update N nodes, we only have to update 1.
    //
    // Flow is:
    // if nobody's ever locked it,
    // or the lock is expired (it was set to expire and it is currently later than that)
    // then acquire a lock

    val lockQuery = runs.query.
      filter(r =>
        r.id === rootId.asRunInstance &&
        r.actionLockedTill.isEmpty || r.actionLockedTill <= now
      ).
      map(r => (r.actionLockedTill, r.lockId)).
      update((Some(lockExpirationTime), Some(newLockId)))

    // unlock the instance and the lock key is the
    // time we expected to lock till
    val unlockQuery = runs.query.
      filter(r =>
        r.id === rootId.asRunInstance &&
        r.lockId === newLockId
      ).
      map(r => (r.actionLockedTill, r.lockId)).
      update((None, None))

    provider.withDB(lockQuery).flatMap(results => {
      // if the lock was acquired, resolve the tree
      // and allow someone to do work with it
      // and attempt a safe unlock
      if (results > 0) {
        logger.debug(s"Acquired lock $newLockId to expire on $lockExpirationTime")

        for {
          data <- getRunTree(rootId).map(block).map(Some(_))
          _ <- provider.withDB(unlockQuery)
        } yield {
          logger.debug(s"Unlocked lock $newLockId")

          data
        }
      } else {
        logger.debug(s"Lock $newLockId is acquired, cannot execute action")
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

case class StepRunDao(stepTreeDao: StepTreeDao, runDao: RunDao)