package io.paradoxical.aetr.core.execution

import io.exceptions.MaxRetriesAttempted
import io.paradoxical.aetr.core.config.ServiceConfig
import io.paradoxical.aetr.core.db.StepsDbSync
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model.{ResultData, RunState}
import javax.inject.Inject
import scala.util.{Failure, Success}

class Completor @Inject()(
  storage: StepsDbSync,
  serviceConfig: ServiceConfig,
  advancer: AdvanceQueuer
) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def complete(runToken: RunToken, data: Option[ResultData]): Boolean = {
    logger.info(s"Completing $runToken with data '${data.map(_.value).getOrElse("")}'")

    def completeSafe(retry: Int): Boolean = {
      if (retry >= serviceConfig.maxAtomicRetries) {
        throw MaxRetriesAttempted()
      }

      val root = storage.loadRun(runToken.rootId)

      val manager = new RunManager(root)

      val updatedRoot = manager.find(runToken.runId).flatMap(run => {
        if (run.state == RunState.Complete) {
          logger.warn("Attempting to double complete! Ignoring")

          None
        } else {
          try {
            manager.complete(run, data)
          } catch {
            case e: Exception =>
              // if we fail to complete due to mapping or reduction errors, fail the entire chain
              manager.setState(runToken.runId, RunState.Error, result = Some(Some(ResultData(e.getMessage))))
          }

          Some(manager.root)
        }
      })

      // TODO: try upsert only from node -> parent -> root
      updatedRoot.map(root => storage.tryUpsertRun(root) match {
        case Failure(exception) =>
          logger.warn("Unable to upsert run, retrying", exception)

          completeSafe(retry + 1)
        case Success(value) =>
          logger.info(s"Upserted root ${root.id}")

          if (manager.state == RunState.Pending) {
            advancer.enqueue(root.rootId)
          }
      }).exists(_ => true)
    }

    completeSafe(1)
  }
}

