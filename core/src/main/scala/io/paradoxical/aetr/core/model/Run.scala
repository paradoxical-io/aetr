package io.paradoxical.aetr.core.model

import io.paradoxical.global.tiny.{LongValue, UuidValue}
import java.time.Instant
import java.util.UUID

case class Run(
  id: RunInstanceId,
  var children: Seq[Run],
  rootId: RootId,
  repr: StepTree,
  version: Version = Version(1),
  createdAt: Instant = Instant.now(),
  updatedAt: Instant = Instant.now(),
  stateChangedAt: Instant = Instant.now(),
  var parent: Option[Run] = None,
  var state: RunState = RunState.Pending,
  var result: Option[ResultData] = None
) {
  override def toString: String = repr.toString
}

trait RunId extends UuidValue with Product

case class RootId(value: UUID) extends RunId {
  def asRunInstance = RunInstanceId(value)
}
case class RunInstanceId(value: UUID) extends RunId

case class Version(value: Long) extends LongValue {
  def inc(): Version = Version(value + 1)
}

case class Actionable(run: Run, action: Action, previousResult: Option[ResultData])