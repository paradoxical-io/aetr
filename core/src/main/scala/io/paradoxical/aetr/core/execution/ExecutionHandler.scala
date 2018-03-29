package io.paradoxical.aetr.core.execution

import io.config.ServiceConfig
import io.exceptions.MaxRetriesAttempted
import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.steps.graph.RunManager
import java.net.URL
import javax.inject.Inject
import scala.util.{Failure, Success}

case class RunToken(runId: RunId, rootId: RunId)

trait UrlExecutor {
  def execute(token: RunToken, url: URL, data: Option[String]): Unit
}

class ExecutionHandler @Inject()(urlExecutor: UrlExecutor) {
  def execute(actionable: Actionable): Unit = {
    val runToken = createRunToken(actionable.run)

    actionable.action.execution match {
      case ApiExecution(url) =>
        urlExecutor.execute(runToken, url, actionable.previousResult)
      case NoOp() =>
    }
  }

  private def createRunToken(run: Run): RunToken = {
    RunToken(run.id, run.root)
  }
}

class Completor @Inject()(storage: Storage, serviceConfig: ServiceConfig) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def complete(runToken: RunToken): Unit = {
    def completeSafe(retry: Int): Unit = {
      if (retry >= serviceConfig.maxAtomicRetries) {
        throw MaxRetriesAttempted()
      }

      val root = storage.loadRun(runToken.rootId)

      new RunManager(root).setState(runToken.runId, StepState.Complete)

      storage.tryUpsertRun(root) match {
        case Failure(exception) =>
          logger.warn("Unable to upsert run, retrying", exception)

          completeSafe(retry + 1)
        case Success(value) =>
          logger.info(s"Upserted root ${root.id}")
      }
    }
  }
}
