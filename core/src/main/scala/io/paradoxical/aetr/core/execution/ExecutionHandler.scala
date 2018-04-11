package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.db.StepsDbSync
import io.paradoxical.aetr.core.execution.api.UrlExecutor
import io.paradoxical.aetr.core.model._
import javax.inject.Inject
import scala.util.{Success, Try}

class ExecutionHandler @Inject()(storage: StepsDbSync, urlExecutor: UrlExecutor) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def execute(actionable: Actionable): Try[ExecutionResult] = {
    val root = storage.getRunTree(actionable.run.rootId)

    val runToken = createRunToken(actionable.run)

    logger.info(s"Executing $actionable with runtoken $runToken")

    val result = actionable.action.execution match {
      case ApiExecution(url) =>
        urlExecutor.
          execute(runToken, url, actionable.previousResult).
          map(result => ExecutionResultState(Some(result), RunState.Executing))
      case NoOp() =>
        Success(ExecutionResultState(result = None, RunState.Complete))
    }

    val resultState = result.map(_.state).getOrElse(RunState.Error)

    try {
      if (storage.trySetRunState(actionable.run.id, root, resultState)) {
        logger.info(s"Set run state of $actionable to $resultState")
      } else {
        logger.info(s"Runs tate of $actionable was already set!")
      }
    } catch {
      case e: Exception =>
        logger.error(s"Unable to set run state for $actionable to $resultState")

        throw e
    }

    result.map(_.result.getOrElse(EmptyExecutionResult))
  }

  private def createRunToken(run: Run): RunToken = {
    RunToken(run.id, run.rootId)
  }
}

trait ExecutionResult
case object EmptyExecutionResult extends ExecutionResult
case class ArbitraryStringExecutionResult(content: String) extends ExecutionResult


case class ExecutionResultState(result: Option[ExecutionResult], state: RunState)