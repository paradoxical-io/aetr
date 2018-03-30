package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.db.dao.tables._
import io.paradoxical.aetr.core.model._
import io.paradoxical.rdb.slick.providers.SlickDBProvider
import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StepDb @Inject()(
  provider: SlickDBProvider,
  dataMappers: DataMappers,
  steps: Steps,
  runs: Runs,
  children: StepChildren,
  composer: StepTreeComposer,
  runDaoManager: RunDaoManager
)(implicit executionContext: ExecutionContext) {

  import dataMappers._
  import provider.driver.api._

  def getTree(stepTreeId: StepTreeId): Future[StepTree] = {
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

  def getRun(rootId: Root): Future[Run] = {
    val relatedToRoot = runs.query.filter(_.root === rootId).result

    provider.withDB {
      relatedToRoot
    }.flatMap(data => {
      val root = data.find(_.id.value == rootId.value).get

      getTree(root.stepTreeId).map(tree => {
        runDaoManager.reconstitute(rootId, data, tree)
      })
    })
  }

  def setRunState(
    id: RunInstanceId,
    version: Version,
    state: RunState,
    result: Option[ResultData]
  ): Future[Boolean] = {
    val now = Instant.now()

    val update = runs.updateWhere(
      r => r.id === id && r.version === version,
      run => (run.version, run.state, run.result, run.lastUpdatedAt, run.stateUpdatedAt),
      (version.inc(), state, result, now, now)
    )

    provider.withDB(update).map(updated => updated == 1)
  }

  private def upsertIfVersion(dao: RunDao): DBIO[Int] = {
    for {
      existing <- runs.query.filter(_.id === dao.id).result.headOption
      row = {
        if (existing.isEmpty) {
          dao.copy(version = dao.version.inc())
        } else if (existing.isDefined && existing.exists(_.version.value == dao.version.value)) {
          dao.copy(version = dao.version.inc())
        } else {
          throw VersionMismatchError()
        }
      }
      result <- runs.query.insertOrUpdate(row)
    } yield {
      result
    }
  }
}

case class VersionMismatchError() extends RuntimeException()