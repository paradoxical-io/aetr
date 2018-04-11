package io.paradoxical.aetr.core.graph

import io.paradoxical.aetr.core.model._

class RunManager(val root: Run) {
  sync(root)

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

  def completeAll(result: Option[ResultData] = None): Unit = {
    flatten.foreach(_.state = RunState.Complete)

    complete(root, result)
  }

  def complete(runInstanceId: RunInstanceId, result: Option[ResultData]): Unit = {
    find(runInstanceId).foreach(complete(_, result))
  }

  def complete(run: Run, result: Option[ResultData] = None): Unit = {
    run.state = RunState.Complete

    run.output = result

    run.parent.foreach(sync)
  }

  def state: RunState = {
    determineState(root)
  }

  private def sync(run: Run): Unit = {
    run.state = determineState(run)

    run.output = getResult(run)

    run.parent.foreach(sync)
  }

  def setState(run: RunId, state: RunState, result: Option[Option[ResultData]] = None): Unit = {
    find(run).foreach(r => {
      r.state = state

      if(result.isDefined) {
        r.output = result.get
      }

      sync(r)
    })
  }

  private def determineState(run: Run): RunState = {
    if (run.children.isEmpty) {
      run.state
    } else {
      if (run.children.forall(_.state == RunState.Complete)) {
        RunState.Complete
      } else if (run.children.exists(_.state == RunState.Error)) {
        RunState.Error
      } else if (run.children.exists(_.state == RunState.Executing)) {
        RunState.Executing
      } else {
        RunState.Pending
      }
    }
  }

  def getFinalResult: Option[ResultData] = getResult(root)

  def getResult(run: Run): Option[ResultData] = {
    if (run.children.isEmpty) {
      run.output
    } else {
      if (determineState(run) == RunState.Complete) {
        run.repr match {
          case x: Parent =>
            x match {
              case p: SequentialParent =>
                run.children.lastOption.flatMap(_.output)
              case p: ParallelParent =>
                val results = run.children.flatMap(_.output)

                p.reducer.reduce(results).map(p.mapper.map)
            }
          case action: Action => run.output.map(action.mapper.map)
        }
      } else {
        None
      }
    }
  }

  def next(): Seq[Actionable] = {
    next(root, root.input)
  }

  private def next(run: Run, data: Option[ResultData]): Seq[Actionable] = {
    run.repr match {
      case x: Parent =>
        x match {
          case _: SequentialParent =>
            val previousResult =
              if (run.children.forall(_.state == RunState.Pending)) {
                // nobody has completed so if a special seed was sent, use that
                data
              } else {
                // find the last one who completed and take their result
                // A matched child that maps its result applies its data mapper here.
                // NOTE that mappers are not applied to root seed data
                run.children.filter(_.state == RunState.Complete).lastOption.flatMap(run => {
                  run.repr match {
                    case canMap: MapsResult => run.output.map(canMap.mapper.map)
                    case _ => run.output
                  }
                })
              }

            run.children.find(_.state == RunState.Pending).map(next(_, previousResult)).getOrElse(Nil)
          case _: ParallelParent =>
            // all we can do is use the seed from whatever the last data was
            run.children.filter(_.state == RunState.Pending).flatMap(next(_, data))
        }
      case x: Action if run.state == RunState.Pending =>
        List(Actionable(run, x, data))
    }
  }
}
