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

    try {
      val runToken = createRunToken(actionable.run)

      logger.info(s"Executing $actionable with runtoken $runToken")

      val result = actionable.action.execution match {
        case ApiExecution(url) =>
          urlExecutor.execute(runToken, url, actionable.previousResult).
            map(result => (result, RunState.Executing))
        case NoOp() =>
          Success((EmptyExecutionResult, RunState.Complete))

      val resultState = result.map(_._2).getOrElse(RunState.Error)

      storage.trySetRunState(actionable.run.id, root, resultState)

      result.map(_._1)
    }
  }

  private def createRunToken(run: Run): RunToken = {
    RunToken(run.id, run.rootId)
  }
}

trait ExecutionResult
case object EmptyExecutionResult extends ExecutionResult
case class ArbitraryStringExecutionResult(content: String) extends ExecutionResult

