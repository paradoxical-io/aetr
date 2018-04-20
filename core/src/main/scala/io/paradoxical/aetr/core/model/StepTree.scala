package io.paradoxical.aetr.core.model

import io.paradoxical.global.tiny.{StringValue, UuidValue}
import java.util.UUID

sealed trait StepTree extends MapsResult {
  val id: StepTreeId

  val name: NodeName
}

sealed trait Parent extends StepTree {
  val children: List[StepTree]

  def addTree(stepTree: StepTree): Parent
}

/**
 * Mappers can only be applied to children within a parent
 *
 * For example, if you have a tree of
 *
 * {{{
 *    A
 *  /   \
 * B     C
 * }}}
 *
 * We want to apply a mapper for B -> C.  However, this only is allowed if A is a sequential
 * Parallels don't map to each other, but are instead reduced by their parents.
 *
 * However, if we had a tree like:
 * {{{
 *        X
 *      /   \
 *    A       Y
 *  /   \
 * B     C
 * }}}
 *
 * And lets say A is parallel and X is sequential
 *
 * B and C don't get mappers, since A is parallel. However A gets a mapper since X is sequential (to map its
 * final reduced value to Y)
 */
trait MapsResult {
  val mapper: Option[Mapper]

  def withMapper(m: Option[Mapper]): StepTree
}

trait ReducesResult {
  val reducer: Reducer = Reducers.NoOp()
}

case class SequentialParent(
  id: StepTreeId = StepTreeId.next,
  name: NodeName,
  children: List[StepTree] = Nil,
  mapper: Option[Mapper] = None
) extends Parent {
  override def addTree(stepTree: StepTree): Parent = {
    copy(children = children :+ stepTree)
  }

  override def withMapper(m: Option[Mapper]): StepTree = {
    copy(mapper = m)
  }
}

case class ParallelParent(
  id: StepTreeId = StepTreeId.next,
  name: NodeName,
  children: List[StepTree] = Nil,
  mapper: Option[Mapper] = None,
  override val reducer: Reducer = Reducers.NoOp()
) extends Parent with ReducesResult {
  override def addTree(stepTree: StepTree): Parent = {
    copy(children = children :+ stepTree)
  }


  override def withMapper(m: Option[Mapper]): StepTree = {
    copy(mapper = m)
  }
}

case class Action(
  id: StepTreeId = StepTreeId.next,
  name: NodeName,
  execution: Execution = NoOp(),
  mapper: Option[Mapper] = None
) extends StepTree {

  override def withMapper(m: Option[Mapper]): StepTree = {
    copy(mapper = m)
  }
}

object StepTreeId {
  def next = StepTreeId(UUID.randomUUID())
}

case class StepTreeId(value: UUID) extends UuidValue

case class NodeName(value: String) extends StringValue

case class ResultData(value: String) extends StringValue
