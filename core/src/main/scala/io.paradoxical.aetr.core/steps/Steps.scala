package io.paradoxical.aetr.core.task

import io.paradoxical.aetr.core.steps.StepState
import io.paradoxical.global.tiny.{StringValue, UuidValue}
import java.net.URL
import java.util.UUID

// json subtypes
sealed abstract class Step {
  val id: StepId

  val name: StepName

  val next: Option[SubSteps]

  def withNext(subSteps: SubSteps): Step
}

case class ApiStep(id: StepId, name: StepName, next: Option[SubSteps], url: URL) extends Step {
  override def withNext(subSteps: SubSteps): Step = copy(next = Some(subSteps))
}

// json subtypes
sealed abstract class SubSteps {
  val next: List[Step]
}

case class SequentialSubSteps(next: List[Step]) extends SubSteps
case class ParallelSubSteps(next: List[Step]) extends SubSteps

/**
 * Loads a step from the step store
 */
trait StepLoader {
  def getStep(id: StepId): Option[Step]

  def upsertStep(step: Step): Step

  def upsertStepInstance(stepInstance: StepInstance): StepInstance

  def getStepInstance(id: StepInstanceId): Option[StepInstance]
}

/**
 * Starts a step node and advances step nodes from callbacks
 */
trait StepProcessor {
  def execute(step: Step, data: Option[StepInstanceResult]): StepInstance

  def advance(from: StepInstanceId, result: Option[StepInstanceResult]): List[StepInstance]
}

object StepInstance {
  def newInstance(step: Step): StepInstance = {
    StepInstance(
      id = StepInstanceId(UUID.randomUUID()),
      state = StepState.Pending,
      source = step.id,
      previous = None,
      nextStep = None,
      result = None
    )
  }
}

/**
 * Metadata about an exeuction of a step
 *
 * @param id       The id of the exeuction
 * @param source   The step source this came from
 * @param previous The previous step instance in the chain
 * @param nextStep The optional next step to execute when this is done
 */
case class StepInstance(
  id: StepInstanceId,
  state: StepState,
  source: StepId,
  previous: Option[StepInstanceId],
  nextStep: Option[SubSteps],
  result: Option[StepInstanceResult]
)

object StepId {
  def next = StepId(UUID.randomUUID())
}

case class StepId(value: UUID) extends UuidValue
case class StepInstanceId(value: UUID) extends UuidValue
case class StepName(value: String) extends StringValue
case class StepInstanceResult(value: String) extends StringValue
