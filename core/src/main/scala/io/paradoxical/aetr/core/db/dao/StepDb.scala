package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.db.dao.tables.{StepChildren, Steps}
import io.paradoxical.aetr.core.model.{StepTree, StepTreeId}
import io.paradoxical.rdb.slick.providers.SlickDBProvider
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StepDb @Inject()(
  provider: SlickDBProvider,
  dataMappers: DataMappers,
  steps: Steps,
  children: StepChildren,
  composer: StepTreeComposer
)(implicit executionContext: ExecutionContext) {

  import dataMappers._
  import provider.driver.api._

  def getTree(stepTreeId: StepTreeId): Future[StepTree] = {
    val idQuery = sql"""
                     with recursive getChild(kids) as (
                       select ${stepTreeId}
                       union all
                       select child_id from step_children
                       join getChild on kids = step_children.id
                     )
                     select * from getChild""".as[StepTreeId]

    val nodesQuery = for {
      ids <- idQuery
      treeNodes <- steps.query.filter(_.id inSet ids).result
      treeChildren <- children.query.filter(_.id inSet ids).result
    } yield {
      (treeChildren, treeNodes)
    }

    provider.withDB(nodesQuery.withPinnedSession).map {
      case (ids, nodes) =>
        val allSteps = composer.reconstitute(nodes, ids)

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
}
