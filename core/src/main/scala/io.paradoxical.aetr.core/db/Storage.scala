package io.paradoxical.aetr.core.db

import io.paradoxical.aetr.core.model._

trait Storage {
  def upsertSteps(stepTree: StepTree): Unit

  def deleteSteps(stepTree: StepTree): Unit

  def getSteps(stepTreeId: StepTreeId): StepTree

  def upsertRun(run: Run): Unit

  def deleteRun(run: Run): Unit

  def loadRun(root: RunId): Run
}

trait TreeLinker {
  def addChild(parent: StepTreeId, stepTreeId: StepTree): Unit

  def removeChild(parent: StepTreeId, id: StepTreeId): Unit
}