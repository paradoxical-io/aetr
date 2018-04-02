package io.paradoxical.aetr.core.server.controllers

import com.twitter.finatra.request.RouteParam
import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.controllers.conversion.DtoConvertors
import io.paradoxical.finatra.Framework
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RunsController @Inject()(db: StepDb, converters: DtoConvertors)(implicit executionContext: ExecutionContext) extends Framework.RestApi {
  postWithDoc("/api/v1/runs/step/:id") {
    _.description("Start a new run from a step").request[CreateRunRequest].responseWith[CreateRunResult](status = 200)
  } { r: CreateRunRequest =>
    db.getStep(r.id).
      map(stepTree => new RunManager(stepTree).root).
      flatMap(db.upsertRun).
      map(CreateRunResult)
  }

  getWithDoc("/api/v1/runs/:id") {
    _.description("Get run id state").request[GetRunRequest].responseWith[GetRunResult](status = 200)
  } { r: GetRunRequest =>
    db.getRun(RunInstanceId(r.id)).map(dao => GetRunResult(state = dao.state, result = dao.result))
  }
}

case class CreateRunRequest(@RouteParam id: StepTreeId)
case class CreateRunResult(id: RunInstanceId)

case class GetRunRequest(@RouteParam id: UUID)

case class GetRunResult(state: RunState, result: Option[ResultData])
