package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model.{Root, Run, StepState}
import javax.inject.Inject

class Advancer @Inject()(storage: Storage, executionHandler: ExecutionHandler) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def advanceAll(): Unit = {
    val pendingRuns = storage.findRuns(StepState.Pending)

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
    require(run.id == run.root, s"Only the root can advance, but got ${run.id}!")

    new RunManager(run).next().foreach(executionHandler.execute)
  }
}
