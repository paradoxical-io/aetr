package io.paradoxical.aetr

import io.paradoxical.aetr.core.graph.{RunManager, TreeManager}
import io.paradoxical.aetr.core.model._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scala.util.Random

class StepStateTests extends FlatSpec with Matchers with MockitoSugar {

  trait ActionList {
    val rootId = StepTreeId.next

    val action1: Action = Action(id = StepTreeId.next, NodeName("action1"), mapper = Some(Mappers.Function(res => ResultData(res.value + "_mapped"))))
    val action2 = Action(id = StepTreeId.next, NodeName("action2"))
    val action3 = Action(id = StepTreeId.next, NodeName("action3"))
    val action4 = Action(id = StepTreeId.next, NodeName("action4"))

    val parallelParent = ParallelParent(
      id = StepTreeId.next,
      name = NodeName("parellelParent"),
      reducer = Reducers.Function(s => Some(ResultData(s.map(_.value).mkString(";"))))
    ).addTree(action3).addTree(action4)

    val sequentialParent = SequentialParent(
      name = NodeName("SequentialParent"),
      id = StepTreeId.next,
    ).addTree(action1).addTree(action2)

    val treeRoot = SequentialParent(name = NodeName("root"), id = rootId).addTree(sequentialParent).addTree(parallelParent)
  }

  "Step state" should "sub children" in new ActionList {
    val m = new RunManager(treeRoot)

    def advance(action: Action*) = {
      val nextActions = m.next()

      if (nextActions.isEmpty) {
        assert(m.root.state == RunState.Complete)
      }

      assert(nextActions.map(_.action) == action.toList)

      nextActions.map(_.run).foreach(a => {
        m.setState(a.id, RunState.Executing)

        assert(m.root.state == RunState.Executing)
      })

      nextActions.map(_.run).foreach(m.complete(_))
    }

    advance(action1)
    advance(action2)
    advance(action3, action4)
    advance()
  }

  it should "re-run pending children" in new ActionList {
    val m = new RunManager(treeRoot)

    val all = m.flatten.map(_.run)

    def findByStep(stepTreeId: StepTreeId): Run = {
      all.find(_.repr.id == stepTreeId).get
    }

    def advance(state: RunState, action: Action*) = {
      val nextActions = m.next()

      if (nextActions.isEmpty) {
        assert(m.root.state == RunState.Complete)
      }

      assert(nextActions.map(_.action) == action.toList)

      nextActions.map(_.run).foreach(a => {
        m.setState(a.id, state)

        assert(m.root.state == state)
      })

      nextActions.map(_.run).foreach(m.complete(_))
    }

    advance(RunState.Executing, action1)
    advance(RunState.Executing, action2)
    advance(RunState.Executing, action3, action4)

    m.setState(findByStep(action3.id).id, RunState.Error)

    assert(m.root.state == RunState.Error)

    m.setState(findByStep(action3.id).id, RunState.Pending)

    advance(RunState.Executing, action3)

    advance(RunState.Complete)
  }

  it should "find a node" in new ActionList {
    val manager = new RunManager(treeRoot)

    val allNodes = manager.flatten.map(_.run)

    val toFind = Random.shuffle(allNodes).head

    manager.find(toFind.id).get shouldEqual toFind
  }

  it should "flatten step trees" in new ActionList {
    new TreeManager(treeRoot).flatten shouldEqual List(
      treeRoot, sequentialParent, action1, action2, parallelParent, action3, action4
    )
  }

  it should "flatten run lists" in new ActionList {
    new RunManager(treeRoot).flatten.map(_.run.repr) shouldEqual new TreeManager(treeRoot).flatten
  }

  it should "pass the result of the previous into the current" in new ActionList {
    private val seedData = ResultData("seed")

    val run = new TreeManager(treeRoot).newRun(input = Some(seedData))

    val m = new RunManager(run)

    val actionableAction1 = m.next().head

    assert(actionableAction1.previousResult.contains(seedData))

    val action1Result = Some(ResultData("action1"))

    m.complete(actionableAction1.run, result = action1Result)

    val actionableAction2 = m.next().head

    private val action1MappedResult = ResultData("action1_mapped")

    // make sure the tracked input is set properly on the related run
    assert(m.find(actionableAction2.run.id).get.input.contains(action1MappedResult))

    assert(actionableAction2.previousResult.contains(action1MappedResult))

    private val action2Result = ResultData("action2")

    m.complete(actionableAction2.run, Some(action2Result))

    val parallelActionItems = m.next()

    assert(parallelActionItems.map(_.previousResult) == List(Some(action2Result), Some(action2Result)))

    parallelActionItems.zipWithIndex.foreach { case (a, i) => m.complete(a.run, Some(ResultData(s"p$i"))) }

    m.getFinalResult shouldEqual Option(ResultData("p0;p1"))

    assert(m.root.input.contains(seedData))

    val runNodesByStepLookup = m.flatten.map(x => x.run.repr.id -> x).toMap

    val inputByRun = m.flatten.flatMap(_.run.input)

    assert(inputByRun == List(
      seedData, // root
      seedData, //sequential parent 1
      seedData, // action1
      action1MappedResult, // action2
      action2Result, //parallel parent
      action2Result, // action3
      action2Result // action4
    ))

    // the first sequential node should have its input the same as the root
    assert(runNodesByStepLookup(sequentialParent.id).run.input.contains(seedData))

    // the second sequential node should have its input hte result of the first chain
    // which is the result of the second action
    assert(runNodesByStepLookup(parallelParent.id).run.input.contains(action2Result))
  }

  it should "not re-sync a run if it is in a terminal state" in new ActionList {
    val run = new TreeManager(treeRoot).newRun(input = Some(ResultData("seed")))

    val m1 = new RunManager(run)

    m1.completeAll(Some(ResultData("complete")))

    // simulate the underlying repr changing from the db
    val updatedRepr = run.repr.withMapper(Some(Mappers.Function(_ => ResultData("mapped"))))

    val m2 = new RunManager(run.copy(repr = updatedRepr))

    // because the root was already in a terminal state
    // we should not apply the new mapper
    // given it should already have an output value
    m2.getFinalResult shouldEqual m1.getFinalResult
  }

  it should "propagate errors to the root" in new ActionList {
    val run = new TreeManager(treeRoot).newRun(input = Some(ResultData("seed")))

    val m = new RunManager(run)

    val actionableAction1 = m.next().head

    val failureData = Some(ResultData("foo"))

    m.setState(actionableAction1.run.id, RunState.Error, Some(failureData))

    assert(m.root.state == RunState.Error)
    assert(m.root.output == failureData)
  }
}
