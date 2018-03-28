package com.example

import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.steps.graph.RunManager
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scala.util.Random

class StepStateTests extends FlatSpec with Matchers with MockitoSugar {

  trait ActionList {
    val rootId = StepTreeId.next

    val action1: Action = Action(id = StepTreeId.next, "action1", root = Some(rootId))
    val action2 = Action(id = StepTreeId.next, "action2", root = Some(rootId))
    val action3 = Action(id = StepTreeId.next, "action3", root = Some(rootId))

    val action4 = Action(id = StepTreeId.next, "action4")

    val parallelParent = ParallelParent(id = StepTreeId.next, root = Some(rootId)).addTree(action3).addTree(action4)

    val sequentialParent = SequentialParent(id = StepTreeId.next, root = Some(rootId)).addTree(action1).addTree(action2)

    val root = SequentialParent(id = rootId).addTree(sequentialParent).addTree(parallelParent)
  }

  "Step state" should "sub children" in new ActionList {
    val m = new RunManager(root)

    def advance(action: Action*) = {
      val nextActions = m.next()

      if (nextActions.isEmpty) {
        assert(m.run.state == StepState.Complete)
      }

      assert(nextActions.map(_.action) == action.toList)

      nextActions.map(_.run).foreach(a => {
        m.setState(a.id, StepState.Executing)

        assert(m.run.state == StepState.Executing)
      })

      nextActions.map(_.run).foreach(m.complete)
    }

    advance(action1)
    advance(action2)
    advance(action3, action4)
    advance()
  }

  it should "find a node" in new ActionList {
    val manager = new RunManager(root)

    val allNodes = manager.flatten

    val toFind = Random.shuffle(allNodes).head

    manager.find(toFind.id).get shouldEqual toFind
  }
}
