package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.model._
import java.net.URL
import javax.inject.Inject

case class RunToken(runId: RunId, rootId: RootId)

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
        storage.trySetRunState(actionable.run.id, actionable.run.version, RunState.Complete)
    }

    // if by the time this line runs its already complete the version will be updated
    // and this line will no-op. This is because we are using the version _pre_ execution
    storage.trySetRunState(actionable.run.id, actionable.run.version, RunState.Executing)
  }

  private def createRunToken(run: Run): RunToken = {
    RunToken(run.id, run.rootId)
  }
}

