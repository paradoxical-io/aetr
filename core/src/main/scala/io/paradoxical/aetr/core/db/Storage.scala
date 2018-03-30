package io.paradoxical.aetr.core.db

import io.paradoxical.aetr.core.model._
import io.paradoxical.global.tiny.UuidValue
import java.util.UUID
import scala.util.Try

trait Storage {
  def upsertSteps(stepTree: StepTree): Unit

  def deleteSteps(stepTree: StepTree): Unit

  def getSteps(stepTreeId: StepTreeId): StepTree

  /**
   * Sets the state of a run id IF its allowed to move to that state
   * For example, we dont want to move a state to Executing if its already marked as Complete
   *
   * @param runId
   * @param stepState
   */
  def trySetRunState(runId: RunId, stepState: RunState): Unit

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
  def loadRun(root: Root): Run

  /**
   * Find runs in the current state (from the root)
   *
   * @param state
   * @return
   */
  def findRuns(state: RunState): List[Run]

  /**
   * Acquire runs for processing
   *
   * This should find runs that are in a Pending state
   * and mark a TTL for work
   *
   * @return
   */
  def tryAcquire(run: Run): Option[AcquisitionLock[Run]]

  /**
   * Release the lock
   *
   * @param id
   */
  def releaseRun(id: AcquisitionLockId): Unit

  /**
   * Find runs related to a step tree
   *
   * @param stepTreeId
   * @return
   */
  def listRunsRelated(stepTreeId: StepTreeId): List[Run]
}

case class AcquisitionLockId(value: UUID) extends UuidValue

case class AcquisitionLock[T](id: AcquisitionLockId, data: T)