package com.example

import io.paradoxical.aetr.core.steps.StepState
import io.paradoxical.aetr.core.steps.execution._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class StepStateTests extends FlatSpec with Matchers with MockitoSugar {

  "Step state" should "sub children" in {
    val action = Action("action1", NoOp)
    val action2 = Action("action2", NoOp)
    val action3 = Action("action3", NoOp)
    val action4 = Action("action4", NoOp)

    val parallelParent = ParallelParent(List(action3, action4))

    val sequentialParent = SequentialParent(List(action, action2))

    val root = SequentialParent(List(sequentialParent, parallelParent))

    root.state shouldEqual StepState.Pending

    root.getNext(None).map(_.action) shouldEqual List(action)

    action.complete()

    root.getNext(None).map(_.action) shouldEqual List(action2)

    action2.complete()

    root.getNext(None).map(_.action) shouldEqual List(action3, action4)

    action3.complete()

    root.getNext(None).map(_.action) shouldEqual List(action4)

    action4.complete()

    root.state shouldEqual StepState.Complete
  }
}
