package io.paradoxical.aetr.core.graph

import io.paradoxical.aetr.core.model._

class RunManager(val root: Run) {
  def this(stepTree: StepTree) {
    this(new TreeManager(stepTree).newRun())
  }

  def flatten: List[Run] = {
    def all0(curr: Run, acc: List[Run]): List[Run] = {
      if (curr.children.isEmpty) {
        curr :: acc
      } else {
        curr :: curr.children.flatMap(c => all0(c, acc)).toList
      }
    }

    all0(root, Nil)
  }

  def find(id: RunId): Option[Run] = {
    def find0(curr: Run): Option[Run] = {
      if (curr.id == id) {
        Some(curr)
      } else {
        curr.children.view.map(find0).find(_.isDefined).flatten
      }
    }

    find0(root)
  }

  def complete(run: Run, result: Option[ResultData] = None): Unit = {
    run.state = StepState.Complete

    run.result = result

    run.parent.foreach(sync)
  }

  def state: StepState = {
    determineState(root)
  }

  private def sync(run: Run): Unit = {
    run.state = determineState(run)

    run.result = getResult(run)

    run.parent.foreach(sync)
  }

  def setState(run: RunId, state: StepState): Unit = {
    find(run).foreach(r => {
      r.state = state

      sync(r)
    })
  }

  private def determineState(run: Run): StepState = {
    if (run.children.isEmpty) {
      run.state
    } else {
      if (run.children.forall(_.state == StepState.Complete)) {
        StepState.Complete
      } else if (run.children.exists(_.state == StepState.Error)) {
        StepState.Error
      } else if (run.children.exists(_.state == StepState.Executing)) {
        StepState.Executing
      } else {
        StepState.Pending
      }
    }
  }

  def getFinalResult: Option[ResultData] = getResult(root)

  def getResult(run: Run): Option[ResultData] = {
    if (run.children.isEmpty) {
      run.result
    } else {
      if (determineState(run) == StepState.Complete) {
        run.repr match {
          case x: Parent =>
            x match {
              case _: SequentialParent =>
                run.children.lastOption.flatMap(_.result)
              case p: ParallelParent =>
                val results = run.children.flatMap(_.result)

                p.reducer(results)
            }
          case _: Action => run.result
        }
      } else {
        None
      }
    }
  }

  def next(seed: Option[ResultData] = None): Seq[Actionable] = {
    next(root, seed)
  }

  private def next(run: Run, data: Option[ResultData]): Seq[Actionable] = {
    run.repr match {
      case x: Parent =>
        x match {
          case _: SequentialParent =>
            val previousResult =
              if (run.children.forall(_.state == StepState.Pending)) {
                // nobody has completed so if a special seed was sent, use that
                data
              } else {
                // find the last one who completed and take their result
                run.children.filter(_.state == StepState.Complete).lastOption.flatMap(_.result)
              }

            run.children.find(_.state == StepState.Pending).map(next(_, previousResult)).getOrElse(Nil)
          case _: ParallelParent =>
            // all we can do is use the seed from whatever the last data was
            run.children.filter(_.state == StepState.Pending).flatMap(next(_, data))
        }
      case x: Action if run.state == StepState.Pending =>
        List(Actionable(run, x, data))
    }
  }
}