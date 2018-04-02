package io.paradoxical.aetr.core.server.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.RouteParam
import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.controllers.conversion.DtoConvertors
import io.paradoxical.finatra.Framework
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PingController extends Framework.RestApi {
  getWithDoc("/ping") {
    _.description("Ping API")
  } { _: Request =>
    "pong"
  }
}

class StepsController @Inject()(db: StepDb, converters: DtoConvertors)(implicit executionContext: ExecutionContext) extends Framework.RestApi {
  import DtoConvertors._

  getWithDoc[GetStepsId, Future[StepsSlimDto]]("/api/v1/steps/:id/slim") {
    _.description("Get a particular step but not nested").request[GetStepsId].responseWith[StepsSlimDto](status = 200)
  } { r: GetStepsId =>
    db.getStep(r.id).map(converters.fromStep).map(_.toSlim)
  }

  getWithDoc[GetStepsId, Future[StepsFatDto]]("/api/v1/steps/:id") {
    _.description("Get a full step tree").request[GetStepsId].responseWith[StepsFatDto](status = 200)
  } { r: GetStepsId =>
    db.getStep(r.id).map(converters.fromStep)
  }

  getWithDoc("/api/v1/steps/") {
    _.description("Get all step roots").responseWith[List[StepsRootDto]](status = 200)
  } { _: Request =>
    db.getRootSteps().map(_.map(converters.fromStep)).map(_.map(item => {
      StepsRootDto(
        id = item.id,
        name = item.name,
        stepType = item.stepType
      )
    }))
  }

  putWithDoc("/api/v1/steps") {
    _.description("Upsert steps").request[StepsSlimDto].responseWith[Unit](status = 200)
  } { r: StepsSlimDto =>
    converters.toStep(r).map(db.upsertStep)
  }
}

case class GetStepsId(@RouteParam id: StepTreeId)

case class StepsSlimDto(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType,
  root: Option[StepTreeId],
  action: Option[Execution],
  children: Option[List[StepTreeId]]
)

case class StepsFatDto(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType,
  root: Option[StepTreeId],
  action: Option[Execution],
  children: Option[List[StepsFatDto]]
)

case class StepsRootDto(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType
)

