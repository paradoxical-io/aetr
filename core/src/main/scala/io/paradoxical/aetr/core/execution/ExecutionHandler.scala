package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.db.StepsDbSync
import io.paradoxical.aetr.core.execution.api.UrlExecutor
import io.paradoxical.aetr.core.model._
import javax.inject.Inject
import scala.util.{Failure, Success, Try}

class ExecutionHandler @Inject()(storage: StepsDbSync, urlExecutor: UrlExecutor) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def execute(actionable: Actionable): Try[ExecutionResult] = {
    try {
      val runToken = createRunToken(actionable.run)

      logger.info(s"Executing $actionable with runtoken $runToken")

      val result = actionable.action.execution match {
        case ApiExecution(url) =>
          urlExecutor.execute(runToken, url, actionable.previousResult)
        case NoOp() =>
          storage.trySetRunState(actionable.run.id, actionable.run.version, RunState.Complete)
          Success(EmptyExecutionResult)
      }

      // if by the time this line runs its already complete the version will be updated
      // and this line will no-op. This is because we are using the version _pre_ execution
      Try(
        storage.trySetRunState(actionable.run.id, actionable.run.version, RunState.Executing)
      ).flatMap(_ => result)
    } catch {
      case e: Exception =>
        logger.error("Unable to process actionable", e)

        Try(
          storage.trySetRunState(actionable.run.id, actionable.run.version, RunState.Error)
        ).flatMap(_ => {
          Failure(e)
        })
    }
  }

  private def createRunToken(run: Run): RunToken = {
    RunToken(run.id, run.rootId)
  }
}

trait ExecutionResult
case object EmptyExecutionResult extends ExecutionResult
case class ArbitraryStringExecutionResult(content: String) extends ExecutionResult

