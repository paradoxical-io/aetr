package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model.{Root, Run, RunState}
import javax.inject.Inject

class Advancer @Inject()(storage: Storage, executionHandler: ExecutionHandler) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def advanceAll(): Unit = {
    val pendingRuns = storage.findRuns(RunState.Pending)

    pendingRuns.foreach(run => {
      storage.tryAcquire(run).foreach(lock => {
        dispatch(lock.data)

        storage.releaseRun(lock.id)
      })
    })
  }

  def advance(root: Root): Unit = {
    logger.info(s"Advancing root $root")

    dispatch(storage.loadRun(root))
  }

  private def dispatch(run: Run): Unit = {
    require(run.id.value == run.rootId.value, s"Only the root can advance, but got ${run.id}!")

    new RunManager(run).next().foreach(executionHandler.execute)
  }
}
