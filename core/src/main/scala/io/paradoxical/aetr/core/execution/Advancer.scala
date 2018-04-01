package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model.{RootId, Run, RunState}
import javax.inject.Inject
import scala.util.Random

class Advancer @Inject()(storage: Storage, executionHandler: ExecutionHandler) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def advanceAll(): Unit = {
    // shuffle the runs to minimize contention on who tries to acquire a lock
    val pendingRuns = Random.shuffle(storage.findUnlockedRuns(RunState.Pending))

    pendingRuns.foreach(runId => storage.tryLock(runId)(run => {
      if (run.state == RunState.Pending) {
        dispatch(run)
      }
    }))
  }

  def advance(root: RootId): Unit = {
    logger.info(s"Advancing root $root")

    dispatch(storage.loadRun(root))
  }

  private def dispatch(run: Run): Unit = {
    require(run.id.value == run.rootId.value, s"Only the root can advance, but got ${run.id}!")

    new RunManager(run).next().foreach(executionHandler.execute)
  }
}
