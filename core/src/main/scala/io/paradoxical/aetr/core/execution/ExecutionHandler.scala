package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.execution.api.UrlExecutor
import io.paradoxical.aetr.core.model._
import javax.inject.Inject

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

