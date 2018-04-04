package io.paradoxical.aetr.core.model

import io.paradoxical.global.tiny.{StringValue, UuidValue}
import java.util.UUID

sealed trait StepTree {
  val id: StepTreeId

  val name: NodeName

  val root: Option[StepTreeId]
}

sealed trait Parent extends StepTree {
  val children: List[StepTree]

  def addTree(stepTree: StepTree): Parent
}

trait MapsResult {
  def mapper: Mapper
}

case class SequentialParent(
  id: StepTreeId = StepTreeId.next,
  name: NodeName,
  children: List[StepTree] = Nil,
  root: Option[StepTreeId] = None
) extends Parent {
  override def addTree(stepTree: StepTree): Parent = {
    copy(children = children :+ stepTree)
  }
}

case class ParallelParent(
  id: StepTreeId = StepTreeId.next,
  name: NodeName,
  children: List[StepTree] = Nil,
  root: Option[StepTreeId] = None,
  reducer: Reducer = Reducers.Last(),
  mapper: Mapper = Mappers.Identity()
) extends Parent with MapsResult {
  override def addTree(stepTree: StepTree): Parent = {
    copy(children = children :+ stepTree)
  }
}

case class Action(
  id: StepTreeId = StepTreeId.next,
  name: NodeName,
  root: Option[StepTreeId] = None,
  execution: Execution = NoOp(),
  mapper: Mapper = Mappers.Identity()
) extends StepTree with MapsResult

object StepTreeId {
  def next = StepTreeId(UUID.randomUUID())
}

case class StepTreeId(value: UUID) extends UuidValue

case class NodeName(value: String) extends StringValue

case class ResultData(value: String) extends StringValue
