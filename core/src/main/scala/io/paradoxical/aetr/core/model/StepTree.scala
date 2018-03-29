package io.paradoxical.aetr.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import io.paradoxical.global.tiny.{StringValue, UuidValue}
import java.net.URL
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

case class SequentialParent(
  id: StepTreeId,
  name: NodeName,
  children: List[StepTree] = Nil,
  root: Option[StepTreeId] = None
) extends Parent {
  override def addTree(stepTree: StepTree): Parent = {
    copy(children = children :+ stepTree)
  }
}

case class ParallelParent(
  id: StepTreeId,
  name: NodeName,
  children: List[StepTree] = Nil,
  root: Option[StepTreeId] = None,
  reducer: (Seq[ResultData]) => Option[ResultData] = _ => None
) extends Parent {
  override def addTree(stepTree: StepTree): Parent = {
    copy(children = children :+ stepTree)
  }
}

case class Action(
  id: StepTreeId,
  name: NodeName,
  root: Option[StepTreeId] = None,
  execution: Execution = NoOp()
) extends StepTree


object StepTreeId {
  def next = StepTreeId(UUID.randomUUID())
}

case class StepTreeId(value: UUID) extends UuidValue

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  defaultImpl = classOf[NoOp],
  property = "type")
@JsonSubTypes(value = Array(
  new Type(value = classOf[ApiExecution], name = "api")
))
sealed trait Execution
case class ApiExecution(url: URL) extends Execution
case class NoOp() extends Execution

case class NodeName(value: String) extends StringValue

case class ResultData(value: String) extends StringValue