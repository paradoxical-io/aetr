package io.paradoxical.aetr.core.model

import io.paradoxical.aetr.core.execution.RunToken
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
  var parent: Option[Run] = None,
  var state: RunState = RunState.Pending,
  var input: Option[ResultData] = None,
  var output: Option[ResultData] = None
) {
  override def toString: String = repr.toString

  val token: RunToken = RunToken(id, rootId)
}

trait RunId extends UuidValue with Product

case class RootId(value: UUID) extends UuidValue with RunId {
  def asRunInstance = RunInstanceId(value)
}
case class RunInstanceId(value: UUID) extends UuidValue with RunId

case class Version(value: Long) extends LongValue {
  def inc(): Version = Version(value + 1)
}

case class Actionable(run: Run, action: Action, previousResult: Option[ResultData])