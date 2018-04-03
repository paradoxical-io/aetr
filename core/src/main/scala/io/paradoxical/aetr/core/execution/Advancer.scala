package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.db.StepsDbSync
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model.{RootId, Run, RunState}
import javax.inject.Inject
import scala.util.Random

class Advancer @Inject()(storage: StepsDbSync, executionHandler: ExecutionHandler) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def advanceAll(): Unit = {
    // shuffle the runs to minimize contention on who tries to acquire a lock
    val pendingRuns = Random.shuffle(storage.findUnlockedRuns(RunState.Pending))

    pendingRuns.foreach(advance)
  }

  def advance(root: RootId): Unit = {
    storage.tryLock(root)(run => {
      if (run.state == RunState.Pending) {
        if (dispatch(run)) {
          logger.info(s"Advanced $root")
        }
      } else {
        logger.warn(s"Run was asked to advance but is not in a pending state $root - ${run.state}")
      }
    }).getOrElse {
      logger.info(s"Tried to advance $root but could not acquire a lock")
    }
  }

  private def dispatch(run: Run): Boolean = {
    require(run.id.value == run.rootId.value, s"Only the root can advance, but got ${run.id}!")

    new RunManager(run).next().map(executionHandler.execute).exists(_ => true)
  }
}
