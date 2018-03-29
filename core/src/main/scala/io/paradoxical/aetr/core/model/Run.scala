package io.paradoxical.aetr.core.model

import io.paradoxical.global.tiny.{LongValue, UuidValue}
import java.util.UUID

case class Run(
  id: RunId,
  var children: Seq[Run],
  root: Root,
  repr: StepTree,
  version: Version = Version(1),
  var parent: Option[Run] = None,
  var state: StepState = StepState.Pending,
  var result: Option[String] = None
) {
  override def toString: String = repr.toString
}

trait RunId extends UuidValue with Product

case class Root(value: UUID) extends RunId
case class RunInstanceId(value: UUID) extends RunId

case class Version(value: Long) extends LongValue

case class Actionable(run: Run, action: Action, previousResult: Option[String])