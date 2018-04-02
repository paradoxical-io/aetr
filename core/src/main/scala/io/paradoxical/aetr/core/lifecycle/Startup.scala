package io.paradoxical.aetr.core.lifecycle

import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.execution.{AdvanceQueuer, Advancer}
import io.paradoxical.aetr.core.model.RunState
import io.paradoxical.common.extensions.Extensions._
import javax.inject.Inject
import scala.concurrent.ExecutionContext

/**
 * On startup try and advance whatever we can.
 *
 * If the service crashes, or has other transient errors
 * this should catch any orphaned failures
 */
class Startup @Inject()(
  stepsDb: StepDb,
  advanceQueuer: AdvanceQueuer,
  advancer: Advancer
)(implicit executionContext: ExecutionContext) {
  def start(): Unit = {
    stepsDb.findUnlockedRuns(RunState.Pending).map(_.map(advanceQueuer.enqueue)).waitForResult()

    val dequeueThread = new Thread(() => {
      while(true) {
        val dequeue = advanceQueuer.dequeue()

        advancer.advance(dequeue)
      }
    })

    dequeueThread.setDaemon(true)
    dequeueThread.setName("Dequeue-thread")
    dequeueThread.start()
  }
}
