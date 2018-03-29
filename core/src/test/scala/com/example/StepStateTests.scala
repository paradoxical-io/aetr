package com.example

import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.steps.graph.{RunManager, TreeManager}
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

    val parallelParent = ParallelParent(
      id = StepTreeId.next,
      name = "parellelParent",
      root = Some(rootId),
      reducer = s => Some(s.mkString(";"))
    ).addTree(action3).addTree(action4)

    val sequentialParent = SequentialParent(
      name = "SequentialParent",
      id = StepTreeId.next,
      root = Some(rootId)
    ).addTree(action1).addTree(action2)

    val root = SequentialParent(name = "root", id = rootId).addTree(sequentialParent).addTree(parallelParent)
  }

  "Step state" should "sub children" in new ActionList {
    val m = new RunManager(root)

    def advance(action: Action*) = {
      val nextActions = m.next()

      if (nextActions.isEmpty) {
        assert(m.root.state == StepState.Complete)
      }

      assert(nextActions.map(_.action) == action.toList)

      nextActions.map(_.run).foreach(a => {
        m.setState(a.id, StepState.Executing)

        assert(m.root.state == StepState.Executing)
      })

      nextActions.map(_.run).foreach(m.complete(_))
    }

    advance(action1)
    advance(action2)
    advance(action3, action4)
    advance()
  }

  it should "re-run pending children" in new ActionList {
    val m = new RunManager(root)

    val all = m.flatten

    def findByStep(stepTreeId: StepTreeId): Run = {
      all.find(_.repr.id == stepTreeId).get
    }

    def advance(state: StepState, action: Action*) = {
      val nextActions = m.next()

      if (nextActions.isEmpty) {
        assert(m.root.state == StepState.Complete)
      }

      assert(nextActions.map(_.action) == action.toList)

      nextActions.map(_.run).foreach(a => {
        m.setState(a.id, state)

        assert(m.root.state == state)
      })

      nextActions.map(_.run).foreach(m.complete(_))
    }

    advance(StepState.Executing, action1)
    advance(StepState.Executing, action2)
    advance(StepState.Executing, action3, action4)

    m.setState(findByStep(action3.id).id, StepState.Error)

    assert(m.root.state == StepState.Error)

    m.setState(findByStep(action3.id).id, StepState.Pending)

    advance(StepState.Executing, action3)

    advance(StepState.Complete)
  }

  it should "find a node" in new ActionList {
    val manager = new RunManager(root)

    val allNodes = manager.flatten

    val toFind = Random.shuffle(allNodes).head

    manager.find(toFind.id).get shouldEqual toFind
  }

  it should "flatten step trees" in new ActionList {
    new TreeManager(root).flatten shouldEqual List(
      root, sequentialParent, action1, action2, parallelParent, action3, action4
    )
  }

  it should "flatten run lists" in new ActionList {
    new RunManager(root).flatten.map(_.repr) shouldEqual new TreeManager(root).flatten
  }

  it should "pass the result of the previous into the current" in new ActionList {
    val m = new RunManager(root)

    val actionableAction1 = m.next(seed = Some("seed")).head

    assert(actionableAction1.previousResult.contains("seed"))

    m.complete(actionableAction1.run, result = Some("action1"))

    val actionableAction2 = m.next().head

    assert(actionableAction2.previousResult.contains("action1"))

    m.complete(actionableAction2.run, Some("action2"))

    val parallelActionItems = m.next()

    assert(parallelActionItems.map(_.previousResult) == List(Some("action2"), Some("action2")))

    parallelActionItems.zipWithIndex.foreach { case (a, i) => m.complete(a.run, Some(s"p$i")) }

    m.getFinalResult shouldEqual Option("p0;p1")
  }
}
