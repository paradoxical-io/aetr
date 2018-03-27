package com.example.mocks

import io.paradoxical.aetr.core.task._
import scala.collection.mutable

class FakeStepLoader extends StepLoader {
  val stepDb = new mutable.HashMap[StepId, Step]
  val stepInstanceDb = new mutable.HashMap[StepInstanceId, StepInstance]()

  override def getStep(id: StepId): Option[Step] = stepDb.get(id)

  override def upsertStep(step: Step): Step = {
    stepDb.put(step.id, step)

    step
  }

  override def upsertStepInstance(stepInstance: StepInstance): StepInstance = {
    stepInstanceDb.put(stepInstance.id, stepInstance)

    stepInstance
  }

  override def getStepInstance(id: StepInstanceId): Option[StepInstance] = stepInstanceDb.get(id)
}
