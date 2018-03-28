package com.example

import io.paradoxical.aetr.core.steps.execution._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class StepStateTests extends FlatSpec with Matchers with MockitoSugar {

  "Step state" should "sub children" in {
    val action = Action("action1")
    val action2 = Action("action2")
    val action3 = Action("action3")
    val action4 = Action("action4")

    val parallelParent = ParallelParent(List(action3, action4))

    val sequentialParent = SequentialParent(List(action, action2))

    val root = SequentialParent(List(sequentialParent, parallelParent))

    val m = new RunManager(root)

    def advance(action: Action*) = {
      val nextActions = m.next()

      assert(nextActions.map(_.action) == action.toList)

      nextActions.map(_.run).foreach(m.complete)
    }

    advance(action)
    advance(action2)
    advance(action3, action4)
    advance()
  }
}
