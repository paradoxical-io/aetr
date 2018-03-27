package com.example

import com.example.mocks.FakeStepLoader
import io.paradoxical.aetr.core.steps.StepState
import io.paradoxical.aetr.core.steps.execution.{StepExecutor, UrlExecutor}
import io.paradoxical.aetr.core.task._
import java.net.URL
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class StepStateTests extends FlatSpec with Matchers with MockitoSugar {
  def newStep(name: String): Step = {
    ApiStep(
      id = StepId.next,
      name = StepName(name),
      next = None,
      url = new URL("http://fake")
    )
  }

  trait TestSetup {
    val mockUrl = mock[UrlExecutor]

    val db = new FakeStepLoader

    val executor = new StepExecutor(db, mockUrl)
  }

  "Step state" should "process" in new TestSetup {
    val step1 = newStep("one")

    val step2 = newStep("two")

    val root = newStep("root").withNext(SequentialSubSteps(List(step1, step2)))

    val pendingRoot = executor.execute(root, None)
    assert(pendingRoot.source == root.id)

    val pendingStep1 = executor.advance(pendingRoot.id, None)

    assert(pendingStep1.size == 1)
    assert(pendingStep1.head.source == step1.id)

    val pendingStep2 = executor.advance(pendingStep1.head.id, None)

    assert(pendingStep2.size == 1)
    assert(pendingStep2.head.source == step2.id)

    val step2Result = executor.advance(pendingStep2.head.id, None)

    assert(step2Result.isEmpty)

    assert(db.getStepInstance(pendingStep2.head.id).get.state == StepState.Complete)
    assert(db.getStepInstance(pendingStep1.head.id).get.state == StepState.Complete)
    assert(db.getStepInstance(pendingRoot.id).get.state == StepState.Complete)
  }
}
