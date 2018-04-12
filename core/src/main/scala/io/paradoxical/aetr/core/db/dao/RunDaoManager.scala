package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.db.dao.tables.RunDao
import io.paradoxical.aetr.core.graph.{RunManager, TreeManager}
import io.paradoxical.aetr.core.model._
import java.time.Instant
import javax.inject.Inject
import scala.collection.mutable

class RunDaoManager @Inject()() {
  def reconstitute(rootId: RootId, runData: Seq[RunDao], tree: StepTree): Run = {
    val steps: Map[StepTreeId, StepTree] = new TreeManager(tree).flatten.groupBy(_.id).mapValues(_.head)

    val cache = new mutable.HashMap[RunInstanceId, Run]()

    def resolve(r: RunDao): Run = {
      if (cache.contains(r.id)) {
        cache(r.id)
      } else {
        val item = Run(
          id = r.id,
          children = runData.filter(_.parent.exists(_ == r.id)).sortBy(_.order).map(resolve),
          rootId = rootId,
          version = r.version,
          state = r.state,
          output = r.output,
          repr = steps(r.stepTreeId),
          createdAt = r.createdAt,
          updatedAt = r.lastUpdatedAt
        )

        cache.put(item.id, item)

        item
      }
    }

    val runs = runData.map(resolve)

    runs.foreach(run => {
      val parent = runs.find(_.children.exists(_.id == run.id))

      // allowing mutation since this builds the related graph
      // for all other nodes as well
      run.parent = parent
    })

    runs.find(_.id.value == rootId.value).get
  }

  def runToDao(run: Run): List[RunDao] = {
    val flattened = new RunManager(run).flatten

    val now = Instant.now()

    flattened.map(r => {
      RunDao(
        id = RunInstanceId(r.run.id.value),
        root = RunInstanceId(r.run.rootId.value),
        parent = r.run.parent.map(p => RunInstanceId(p.id.value)),
        version = r.run.version,
        stepTreeId = r.run.repr.id,
        state = r.run.state,
        input = r.run.input,
        order = r.order,
        output = r.run.output,
        createdAt = r.run.createdAt,
        lastUpdatedAt = now
      )
    })
  }
}
