package io.paradoxical.aetr.core.db

import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.model._
import io.paradoxical.common.extensions.Extensions._
import javax.inject.Inject
import scala.util.Try

class StepsDbSync @Inject()(stepDb: StepDb) {
  def upsertSteps(stepTree: StepTree): Unit = {
    stepDb.upsertStep(stepTree).waitForResult()
  }

  def deleteSteps(stepTree: StepTree): Unit = {
    stepDb.deleteStepTree(stepTree).waitForResult()
  }

  def getSteps(stepTreeId: StepTreeId): StepTree = {
    stepDb.getStep(stepTreeId).waitForResult()
  }

  /**
   * Try an atomic lock on the id. This doesn't prevent updates
   * or other actions, it only prevents others using the lock mechanism
   * from acting while an id is locked.  This can be used
   * to prevent concurrent processing on an id
   *
   * @param rootId
   * @param run
   * @tparam T
   * @return
   */
  def tryLock[T](rootId: RootId)(run: Run => T): Option[T] = {
    stepDb.lock(rootId)(run).waitForResult()
  }

  /**
   * Atomically update a run
   *
   * @param run
   * @return
   */
  def tryUpsertRun(run: Run): Try[Unit] = {
    Try(stepDb.upsertRun(run).waitForResult())
  }

  def trySetRunState(
    id: RunInstanceId,
    version: Version,
    state: RunState,
    result: Option[ResultData] = None
  ): Boolean = {
    stepDb.setRunState(id, version, state, result).waitForResult()
  }

  /**
   * Given a root id, load a run tree
   *
   * @param root
   * @return
   */
  def loadRun(root: RootId): Run = {
    stepDb.getRunTree(root).waitForResult()
  }

  /**
   * Find runs in the current state (from the root)
   *
   * @param state
   * @return
   */
  def findUnlockedRuns(state: RunState): List[RootId] = {
    stepDb.findUnlockedRuns(state).waitForResult()
  }
}