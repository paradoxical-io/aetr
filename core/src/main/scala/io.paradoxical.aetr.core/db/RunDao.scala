package io.paradoxical.aetr.core.db

import io.paradoxical.aetr.core.model._

case class RunDao(
  id: RunId,
  children: Seq[RunId],
  root: RunId,
  parent: Option[RunId],
  stepTreeId: StepTreeId,
  state: StepState,
  result: Option[String]
)

case class StepTreeDao (
  id: StepTreeId,
  root: Option[StepTreeId],
  stepType: StepType,
  children: List[StepTreeId],
  configJson: Option[String]
)