package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.db.dao.tables.RunDao
import io.paradoxical.aetr.core.graph.{RunManager, TreeManager}
import io.paradoxical.aetr.core.model._
import java.time.Instant
import javax.inject.Inject
import scala.collection.mutable

class RunDaoManager @Inject()() {
  /**
   * Given a hydrated full tree and a set of nodes that represent a run of that tree
   * try and piece the nodes back together into a full fledged tree
   *
   * We start at the run dao that is related to the root, passing around
   * the parent step tree along with each request.  this is because
   * for each branch we want to find the Nth child and re-hydrate that
   *
   * Which again, keeps all the semantic child relationships intact
   *
   * @param rootId
   * @param runData
   * @param root
   * @return
   */
  def reconstitute(rootId: RootId, runData: Seq[RunDao], root: StepTree): Run = {
    val cache = new mutable.HashMap[RunInstanceId, Run]()

    def resolve(r: RunDao, parent: Option[StepTree]): Run = {
      if (cache.contains(r.id)) {
        cache(r.id)
      } else {
        val repr = {
          // steps by themselves aren't the fully hydrated
          // so get them in relation to their parent.
          // if it has no parent, just get it anyways
          parent.map(p => {
            // find our step representation in relation
            // to where we sit in the _parents_ tree.
            // this is because there may be many instances of actions or parents
            // for example, but their mappers and other contetual data
            // varies by their position in the tree.
            // find the tree root given the parents order and the parents tree id
            new TreeManager(p).findAtLevel0(r.order).get
          }).getOrElse(root)
        }

        val item = Run(
          id = r.id,
          children = {
            val runDaoChildren = runData.filter(_.parent.exists(_ == r.id)).sortBy(_.order)

            runDaoChildren.map(child => resolve(child, Some(repr)))
          },
          rootId = rootId,
          version = r.version,
          state = r.state,
          input = r.input,
          output = r.output,
          repr = repr,
          createdAt = r.createdAt,
          updatedAt = r.lastUpdatedAt,
          executedAt = r.executedAt,
          completedAt = r.completedAt
        )

        cache.put(item.id, item)

        item
      }
    }

    // find the root
    val rootDao = runData.find(r => r.id == r.root).get

    // resolve the set of runs into a tree
    val rootRun = resolve(rootDao, parent = None)

    // link all the runs together
    val runs = new RunManager(rootRun).flatten.map(_.run)

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
        executedAt = if (r.run.executedAt.isEmpty && r.run.state == RunState.Executing) Some(Instant.now()) else r.run.executedAt,
        completedAt = if (r.run.completedAt.isEmpty && r.run.state.isTerminalState) Some(Instant.now()) else r.run.completedAt,
        lastUpdatedAt = now
      )
    })
  }
}
