package io.paradoxical.aetr.core.execution

import io.exceptions.MaxRetriesAttempted
import io.paradoxical.aetr.core.config.ServiceConfig
import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model.{ResultData, RunState}
import javax.inject.Inject
import scala.annotation.tailrec
import scala.util.{Failure, Success}

class Completor @Inject()(
  storage: Storage,
  serviceConfig: ServiceConfig,
  advancer: Advancer
) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def complete(runToken: RunToken, data: Option[ResultData]): Unit = {
    @tailrec
    def completeSafe(retry: Int): Unit = {
      if (retry >= serviceConfig.maxAtomicRetries) {
        throw MaxRetriesAttempted()
      }

      val root = storage.loadRun(runToken.rootId)

      val manager = new RunManager(root)

      manager.find(runToken.runId).foreach(run => {
        manager.complete(run, data)
      })

      // TODO: try upsert only from node -> parent -> root
      storage.tryUpsertRun(manager.root) match {
        case Failure(exception) =>
          logger.warn("Unable to upsert run, retrying", exception)

          completeSafe(retry + 1)
        case Success(value) =>
          logger.info(s"Upserted root ${root.id}")

          if(manager.state != RunState.Complete) {
            advancer.advance(root.root)
          }
      }
    }

    completeSafe(1)
  }
}

