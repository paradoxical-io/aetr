package io.paradoxical.aetr.core.steps.execution

import io.paradoxical.aetr.core.steps.StepState

sealed trait StepTree {
  def state: StepState

  def result: Option[String]
}

sealed trait Parent extends StepTree {
  val children: List[StepTree]

  override def state: StepState = {
    if (children.forall(_.state == StepState.Complete)) {
      StepState.Complete
    } else if (children.exists(_.state == StepState.Error)) {
      StepState.Error
    } else if (children.exists(_.state == StepState.Running)) {
      StepState.Running
    } else {
      StepState.Pending
    }
  }

  override def result: Option[String] = {
    if (state == StepState.Complete) {
      children.lastOption.flatMap(_.result)
    } else {
      None
    }
  }

  def getNext(data: Option[String]): List[Actionable]
}

case class SequentialParent(children: List[StepTree]) extends Parent {
  override def getNext(data: Option[String]): List[Actionable] = {
    val payload =
      if (children.forall(_.state == StepState.Pending)) {
        data
      } else {
        children.filter(_.state == StepState.Complete).lastOption.flatMap(_.result)
      }

    children.find(_.state == StepState.Pending).map {
      case x: Parent =>
        x.getNext(payload)
      case x: Action => List(Actionable(x, payload))
    }.getOrElse(Nil)
  }
}


case class ParallelParent(children: List[StepTree]) extends Parent {
  override def getNext(data: Option[String]): List[Actionable] = {
    children.filter(_.state == StepState.Pending).flatMap {
      case x: Parent =>
        x.getNext(data)
      case x: Action => List(Actionable(x, data))
    }
  }
}

case class Action(
  name: String,
  executable: Executable,
  var state: StepState = StepState.Pending,
  var result: Option[String] = None
) extends StepTree {
  def complete() = state = StepState.Complete
}

case class Actionable(action: Action, result: Option[String])

sealed trait Executable {
  def execute(data: Option[String]): Unit
}

case object NoOp extends Executable {
  override def execute(data: Option[String]): Unit = {}
}