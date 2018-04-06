package io.paradoxical.aetr.core.server.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.RouteParam
import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.controllers.conversion.DtoConvertors
import io.paradoxical.finatra.Framework
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

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

  getWithDoc("/api/v1/steps") {
    _.description("Get all step roots").responseWith[List[StepsRootDto]](status = 200)
  } { _: Request =>
    db.getAllSteps().map(_.map(converters.fromStep)).map(_.map(item => {
      StepsRootDto(
        id = item.id,
        name = item.name,
        stepType = item.stepType
      )
    }))
  }

  postWithDoc("/api/v1/steps") {
    _.description("Create a new step node").request[StepsSlimDto].responseWith[CreateStepResponse](status = 200)
  } { r: CreateStepRequest =>

    val slim = StepsSlimDto(
      id = StepTreeId.next,
      name = r.name,
      stepType = r.stepType,
      action = r.action,
      children = r.children
    )

    converters.toStep(slim).map(db.upsertStep).map(_ => CreateStepResponse(slim.id))
  }

  putWithDoc("/api/v1/steps/slim") {
    _.description("Upsert slim steps").request[StepsSlimDto].responseWith[Unit](status = 200)
  } { r: StepsSlimDto =>
    converters.toStep(r).map(db.upsertStep)
  }

  putWithDoc("/api/v1/steps") {
    _.description("Upsert a step tree").request[StepsFatDto].responseWith[Unit](status = 200)
  } { r: StepsFatDto =>
    db.upsertStep(converters.toStep(r))
  }

  postWithDoc("/api/v1/steps/:id/children") {
    _.description("Sets the children for the id").request[AddChildrenRequest].responseWith[Unit](status = 200)
  } { r: AddChildrenRequest =>
    db.setChildren(r.id, r.children)
  }
}

case class AddChildrenRequest(@RouteParam id: StepTreeId, children: List[StepTreeId])

case class GetStepsId(@RouteParam id: StepTreeId)

case class CreateStepResponse(id: StepTreeId)

case class CreateStepRequest(
  name: NodeName,
  stepType: StepType,
  action: Option[Execution],
  children: Option[List[StepTreeId]]
)

case class StepsSlimDto(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType,
  action: Option[Execution],
  children: Option[List[StepTreeId]]
)

case class StepsFatDto(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType,
  action: Option[Execution],
  children: Option[List[StepsFatDto]]
)

case class StepsRootDto(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType
)


