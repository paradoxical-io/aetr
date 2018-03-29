package io.paradoxical.aetr.core.db

import io.paradoxical.aetr.core.model._
import scala.util.Try

trait Storage {
  def upsertSteps(stepTree: StepTree): Unit

  def deleteSteps(stepTree: StepTree): Unit

  def getSteps(stepTreeId: StepTreeId): StepTree

  /**
   * Atomically update a run
   *
   * @param run
   * @return
   */
  def tryUpsertRun(run: Run): Try[Unit]

  def deleteRun(run: Run): Unit

  /**
   * Given a root id, load a run tree
   *
   * @param root
   * @return
   */
  def loadRun(root: RunId): Run

  /**
   * Find runs in the current state (from the root)
   *
   * @param state
   * @return
   */
  def findRuns(state: StepState): List[Run]

  /**
   * Find runs related to a step tree
   *
   * @param stepTreeId
   * @return
   */
  def listRunsRelated(stepTreeId: StepTreeId): List[Run]
}