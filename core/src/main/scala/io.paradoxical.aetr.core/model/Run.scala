package io.paradoxical.aetr.core.model

import io.paradoxical.global.tiny.UuidValue
import java.util.UUID

case class Run(
  id: RunId,
  var children: Seq[Run],
  root: RunId,
  repr: StepTree,
  var parent: Option[Run] = None,
  var state: StepState = StepState.Pending,
  var result: Option[String] = None
) {
  override def toString: String = repr.toString
}

case class RunId(value: UUID) extends UuidValue

case class Actionable(run: Run, action: Action, previousResult: Option[String])