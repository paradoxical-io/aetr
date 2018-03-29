package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.model.Run
import io.paradoxical.aetr.core.steps.graph.RunManager
import javax.inject.Inject

class Advancer @Inject()(storage: Storage, executionHandler: ExecutionHandler) {
  def advanceNext(): Unit = {
    val lock = storage.tryAcquireRuns()

    lock.foreach(lock => {
      lock.data.foreach(dispatch)
    })

    lock.foreach(l => storage.releaseRuns(l.id))
  }

  private def dispatch(run: Run): Unit = {
    new RunManager(run).next().foreach(executionHandler.execute)
  }
}
