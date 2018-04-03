package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.model.RootId
import java.util.concurrent.LinkedBlockingQueue
import javax.inject.Singleton

@Singleton
class AdvanceQueuer {
  private val runsToExecute = new LinkedBlockingQueue[RootId]()

  def dequeue() = runsToExecute.take()

  def enqueue(runId: RootId) = runsToExecute.offer(runId)
}
