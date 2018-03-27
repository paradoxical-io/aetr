package io.paradoxical.aetr.core.task

import io.paradoxical.aetr.core.steps.StepState
import io.paradoxical.global.tiny.{StringValue, UuidValue}
import java.util.UUID

// json subtypes
abstract class Step {
  val id: StepId

  val name: StepName

  val next: SubSteps
}

// json subtypes
abstract class SubSteps {
  val next: List[Step]
}

case class SequentialSubSteps(next: List[Step]) extends SubSteps
case class ParallelSubSteps(next: List[Step]) extends SubSteps

/**
 * Loads a step from the step store
 */
trait StepLoader {
  def loadStep(id: StepId): Step

  def createStep(name: StepName, next: SubSteps): StepId

  def updateStep(step: Step): Unit
}

/**
 * Starts a step node and advances step nodes from callbacks
 */
trait StepProcessor {
  def execute(step: Step): StepInstance

  def advance(from: StepInstanceId): Option[Step]
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
  nextStep: Option[StepId]
)

case class StepId(value: UUID) extends UuidValue
case class StepInstanceId(value: UUID) extends UuidValue
case class StepName(value: String) extends StringValue
