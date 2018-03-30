package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.model._
import java.time.Instant

case class RunDao(
  id: RunId,
  children: Seq[RunId],
  root: Root,
  parent: Option[RunId],
  version: Version,
  stepTreeId: StepTreeId,
  state: StepState,
  result: Option[ResultData],
  createdAt: Instant,
  lastUpdatedAt: Instant,
  stateUpdatedAt: Instant
)

