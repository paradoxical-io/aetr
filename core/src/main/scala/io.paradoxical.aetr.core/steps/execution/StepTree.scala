package io.paradoxical.aetr.core.steps.execution

sealed trait StepTree

sealed trait Parent extends StepTree {
  val children: List[StepTree]
}

case class SequentialParent(children: List[StepTree]) extends Parent

case class ParallelParent(children: List[StepTree]) extends Parent

case class Action(
  name: String
) extends StepTree

sealed trait Executable {
  def execute(data: Option[String]): Unit
}

case object NoOp extends Executable {
  override def execute(data: Option[String]): Unit = {}
}
