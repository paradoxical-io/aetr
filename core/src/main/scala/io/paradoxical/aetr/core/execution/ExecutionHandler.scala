package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.model._
import java.net.URL
import javax.inject.Inject

case class RunToken(runId: RunId, rootId: Root)

trait UrlExecutor {
  // POST url?aetr=runToken <data>
  def execute(token: RunToken, url: URL, data: Option[ResultData]): Unit
}

class ExecutionHandler @Inject()(storage: Storage, urlExecutor: UrlExecutor) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def execute(actionable: Actionable): Unit = {
    val runToken = createRunToken(actionable.run)

    logger.info(s"Executing $actionable with runtoken $runToken")

    actionable.action.execution match {
      case ApiExecution(url) =>
        urlExecutor.execute(runToken, url, actionable.previousResult)
      case NoOp() =>
      // TODO:set this to complete
    }

    // if by the time this line runs its already complete, dont set it to executing
    // if we fail here we will end up retrying the execution
    storage.trySetRunState(actionable.run.id, RunState.Executing)
  }

  private def createRunToken(run: Run): RunToken = {
    RunToken(run.id, run.root)
  }
}

