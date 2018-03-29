package io.paradoxical.aetr.core.execution

import io.config.ServiceConfig
import io.exceptions.MaxRetriesAttempted
import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.model.StepState
import io.paradoxical.aetr.core.steps.graph.RunManager
import javax.inject.Inject
import scala.annotation.tailrec
import scala.util.{Failure, Success}

class Completor @Inject()(storage: Storage, serviceConfig: ServiceConfig) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def complete(runToken: RunToken): Unit = {
    @tailrec
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

    completeSafe(1)
  }
}

