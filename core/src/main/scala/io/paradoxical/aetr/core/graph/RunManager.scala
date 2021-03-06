package io.paradoxical.aetr.core.graph

import io.paradoxical.aetr.core.model._
import scala.collection.mutable

case class OrderedRun(run: Run, parent: Option[Run], order: Long)

class RunManager(val root: Run) {
  if (!root.state.isTerminalState) {
    sync(root)
  }

  def this(stepTree: StepTree) {
    this(new TreeManager(stepTree).newRun())
  }

  /**
   * Flattens a run into an ordered run. An order for a run is related
   * ONLY to the siblings that its part of. This flattening is used
   * to store the sorted order in the database and can be used to reconsitute
   * the relationship between a parent and its child orderings
   *
   * @return
   */
  def flatten: List[OrderedRun] = {
    def all0(curr: Run, parent: Option[Run], acc: List[OrderedRun], order: Long = 0): List[OrderedRun] = {
      if (curr.children.isEmpty) {
        OrderedRun(curr, parent, order) :: acc
      } else {
        OrderedRun(curr, parent, order) :: curr.children.zipWithIndex.flatMap(c => {
          val (child, index) = c

          val children = all0(child, Some(curr), acc, index)

          children
        }).toList
      }
    }

    all0(root, parent = None, acc = Nil)
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
    flatten.map(_.run).foreach(_.state = RunState.Complete)

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

      if (result.isDefined) {
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

  def getFinalResult: Option[ResultData] = {
    root.output
  }

  def getResult(run: Run): Option[ResultData] = {
    if (run.children.isEmpty) {
      run.output
    } else {
      if (determineState(run).isTerminalState) {
        run.repr match {
          case _: SequentialParent =>
            // the last completed child
            // either they are all complete and so the last item is the value we want
            // or one errored out and we want to grab its state
            run.children.filter(_.state.isTerminalState).lastOption.flatMap(lastchild => {
              // if we're complete perform a map
              if (lastchild.state == RunState.Complete) {
                lastchild.output.map(out => {
                  lastchild.repr.mapper.getOrElse(Mappers.Identity()).map(out)
                })
              } else {
                // otherwise return the raw output
                lastchild.output
              }
            })

          case p: ParallelParent =>
            // reduce all parallel errors to be concated together
            if (run.children.exists(_.state == RunState.Error)) {
              Some(run.children.filter(_.state == RunState.Error).flatMap(_.output).mkString(";")).map(ResultData)
            } else {
              val results = run.children.flatMap(_.output)

              p.reducer.reduce(results)
            }

          case action: Action => run.output.map(action.mapper.getOrElse(Mappers.Identity()).map)
        }
      } else {
        None
      }
    }
  }

  def next(): Next = {
    dirtyInputNodes.clear()

    val actionables = next(root, root.input)

    // return the actions to take + the nodes who had their
    // input set as recursing through the tree
    Next(dirtyInputNodes.toSeq, actionables)
  }

  /**
   * Nodes who have their input set on a next query
   */
  private val dirtyInputNodes = new mutable.HashSet[InputSet]()

  private def next(run: Run, data: Option[ResultData]): Seq[Actionable] = {
    if (run.input != data) {
      run.input = data

      // nodes who haven't had their input field set should set
      // it as we recurse through and then we track it
      dirtyInputNodes.add(InputSet(run.id, data))
    }

    run.repr match {
      case x: Parent =>
        x match {
          case _: SequentialParent =>
            val previousResult = determinePreviousResult(run, data)

            run.children.find(_.state == RunState.Pending).map(next(_, previousResult)).getOrElse(Nil)
          case _: ParallelParent =>
            // all we can do is use the seed from whatever the last data was
            run.children.filter(_.state == RunState.Pending).flatMap(next(_, data))
        }
      case x: Action if run.state == RunState.Pending =>
        List(Actionable(run, x, data))
    }
  }

  /**
   * The previous result of a run with its mapping applied
   *
   * @param run
   * @param seed
   * @return
   */
  private def determinePreviousResult(run: Run, seed: Option[ResultData]): Option[ResultData] = {
    if (run.children.forall(_.state == RunState.Pending)) {
      // nobody has completed so if a special seed was sent, use that
      seed
    } else {
      // find the last one who completed and take their result
      // A matched child that maps its result applies its data mapper here.
      // NOTE that mappers are not applied to root seed data
      run.children.filter(_.state == RunState.Complete).lastOption.flatMap(run => {
        run.repr match {
          case canMap: MapsResult => {
            run.output.map(canMap.mapper.getOrElse(Mappers.Identity()).map)
          }
          case _ => run.output
        }
      })
    }
  }
}
