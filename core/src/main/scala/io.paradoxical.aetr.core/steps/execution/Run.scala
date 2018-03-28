package io.paradoxical.aetr.core.steps.execution

import io.paradoxical.aetr.core.steps.StepState
import java.util.UUID


case class Run(
  id: UUID,
  children: Seq[Run],
  repr: StepTree,
  var state: StepState = StepState.Pending,
  var result: Option[String] = None
)

class RunManager(root: StepTree) {
  private lazy val runRoot: Run = newRun(root)

  def run: Run = runRoot

  private def getChildren(node: StepTree): Seq[Run] = {
    node match {
      case p: Parent =>
        p.children.map(newRun)
      case _: Action =>
        Nil
    }
  }

  private def newRun(tree: StepTree): Run = {
    Run(
      UUID.randomUUID(),
      getChildren(tree),
      tree
    )
  }

  def complete(run: Run): Unit = {
    run.state = StepState.Complete

    syncState(runRoot)
  }

  private def syncState(run: Run): Unit = {
    run.state = determineState(run)

    run.children.foreach(syncState)
  }

  private def determineState(run: Run): StepState = {
    if (run.children.isEmpty) {
      run.state
    } else {
      if (run.children.forall(_.state == StepState.Complete)) {
        StepState.Complete
      } else if (run.children.exists(_.state == StepState.Error)) {
        StepState.Error
      } else if (run.children.exists(_.state == StepState.Running)) {
        StepState.Running
      } else {
        StepState.Pending
      }
    }
  }

  def getResult(run: Run): Option[String] = {
    if (run.children.isEmpty) {
      run.result
    } else {
      if (determineState(run) == StepState.Complete) {
        run.children.lastOption.flatMap(getResult)
      } else {
        None
      }
    }
  }

  def next(seed: Option[String] = None): Seq[Actionable] = {
    next(runRoot, seed)
  }

  private def next(run: Run, data: Option[String]): Seq[Actionable] = {
    run.repr match {
      case x: Parent =>
        x match {
          case SequentialParent(_) =>
            val payload =
              if (run.children.forall(_.state == StepState.Pending)) {
                data
              } else {
                run.children.filter(_.state == StepState.Complete).lastOption.flatMap(_.result)
              }

            run.children.find(_.state == StepState.Pending).map(next(_, payload)).getOrElse(Nil)
          case ParallelParent(_) =>
            run.children.filter(_.state == StepState.Pending).flatMap(next(_, data))
        }
      case x: Action if run.state == StepState.Pending =>
        List(Actionable(run, x, None))
    }
  }
}

case class Actionable(run: Run, action: Action, result: Option[String])