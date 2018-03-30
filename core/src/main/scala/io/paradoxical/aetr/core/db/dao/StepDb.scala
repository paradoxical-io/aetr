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
  converters: Converters
)(implicit executionContext: ExecutionContext) {

  import dataMappers._
  import provider.driver.api._

  def resolveSteps(stepTreeId: StepTreeId): Future[StepTree] = {
    val idQuery = sql"""
                     with recursive getChild(kids) as (
                       select ${stepTreeId}
                       union all
                       select child from children
                       join getChild on kids = children.id
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
        val allSteps = converters.resolveSteps(nodes, ids)

        allSteps.find(_.id == stepTreeId).get
    }
  }
}
