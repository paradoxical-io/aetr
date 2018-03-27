package io.paradoxical.aetr.core.task

import scala.collection.mutable

class CycleDetector {
  def detect(root: Step): Boolean = {
    val map = new mutable.HashSet[StepId]()

    def detect0(currentStep: Step): Boolean = {
      if (map.contains(currentStep.id)) {
        true
      } else {
        map.add(currentStep.id)

        currentStep.next.exists(detect0)
      }
    }

    detect0(root)
  }
}
