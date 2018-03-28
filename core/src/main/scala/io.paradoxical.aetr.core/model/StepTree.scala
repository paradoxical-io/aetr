package io.paradoxical.aetr.core.model

import io.paradoxical.global.tiny.UuidValue
import java.util.UUID

sealed trait StepTree {
  val id: StepTreeId

  val root: Option[StepTreeId]
}

sealed trait Parent extends StepTree {
  val children: List[StepTree]

  def addTree(stepTree: StepTree): Parent
}

case class SequentialParent(
  id: StepTreeId,
  children: List[StepTree] = Nil,
  root: Option[StepTreeId] = None
) extends Parent {
  override def addTree(stepTree: StepTree): Parent = {
    copy(children = children :+ stepTree)
  }
}

case class ParallelParent(
  id: StepTreeId,
  children: List[StepTree] = Nil,
  root: Option[StepTreeId] = None
) extends Parent {
  override def addTree(stepTree: StepTree): Parent = {
    copy(children = children :+ stepTree)
  }
}

case class Action(
  id: StepTreeId,
  name: String,
  root: Option[StepTreeId] = None
) extends StepTree

object StepTreeId {
  def next = StepTreeId(UUID.randomUUID())
}

case class StepTreeId(value: UUID) extends UuidValue
