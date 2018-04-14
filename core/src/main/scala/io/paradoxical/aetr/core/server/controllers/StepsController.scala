package io.paradoxical.aetr.core.server.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.exceptions.ConflictException
import com.twitter.finatra.request.RouteParam
import io.paradoxical.aetr.core.db.dao.tables.StepTreeDao
import io.paradoxical.aetr.core.db.dao.{StepChildWithMapper, StepDb}
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.controllers.conversion.DtoConvertors
import io.paradoxical.finatra.Framework
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StepsController @Inject()(db: StepDb, converters: DtoConvertors)(implicit executionContext: ExecutionContext) extends Framework.RestApi {

  import DtoConvertors._
  import io.paradoxical.aetr.core.graph.CycleDetector.Implicits._

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

  getWithDoc("/api/v1/steps/:id/parents") {
    _.description("Get all nodes with this as a child").request[GetStepsId].responseWith[List[StepsRootDto]](status = 200)
  } { r: GetStepsId =>
    db.getParentsOf(r.id).map(related => {
      related.map(toRoot)
    })
  }

  postWithDoc("/api/v1/steps") {
    _.description("Create a new step node").request[GetStepsId].responseWith[CreateStepResponse](status = 200)
  } { r: CreateStepRequest =>

    val slim = StepsSlimDto(
      id = StepTreeId.next,
      name = r.name,
      stepType = r.stepType,
      action = r.action,
      children = r.children.map(_.map(id => StepSlimChild(id))),
      reducer = None
    )

    converters.toStep(slim).map(db.upsertStep).map(_ => CreateStepResponse(slim.id))
  }

  deleteWithDoc("/api/v1/steps/:id") {
    _.description("Deletes a step if it is unused in other steps").
      request[DeleteStepRequest].responseWith[Unit](status = 200)
  } { req: DeleteStepRequest =>
    db.deleteStep(req.id)
  }

  putWithDoc("/api/v1/steps/slim") {
    _.description("Upsert slim steps").request[StepsSlimDto].responseWith[Unit](status = 200)
  } { r: StepsSlimDto =>
    if (r.children.map(_.map(_.id)).exists(_.contains(r.id))) {
      throw ConflictException("Cannot add a child that is also the root")
    }

    converters.toStep(r).map(db.upsertStep)
  }

  putWithDoc("/api/v1/steps") {
    _.description("Upsert a step tree").request[StepsFatDto].responseWith[Unit](status = 200)
  } { r: StepsFatDto =>
    val step = converters.toStep(r)

    if (step.hasCycles) {
      throw ConflictException("Cannot upsert a step with cycles!")
    } else {
      db.upsertStep(step)
    }
  }

  postWithDoc("/api/v1/steps/:id/children") {
    _.description("Sets the children for the id").request[AddChildrenRequest].responseWith[Unit](status = 200)
  } { r: AddChildrenRequest =>
    if (r.children.map(_.id).contains(r.id)) {
      throw ConflictException("Cannot add a child that is also the root")
    }

    db.setChildren(r.id, r.children.map(c => StepChildWithMapper(c.id, c.mapper)))
  }

  private def toRoot(item: StepTreeDao): StepsRootDto = {
    StepsRootDto(
      id = item.id,
      name = item.name,
      stepType = item.stepType
    )
  }
}

case class DeleteStepRequest(@RouteParam id: StepTreeId)

case class AddChildrenRequest(@RouteParam id: StepTreeId, children: List[StepSlimChild])

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
  children: Option[List[StepSlimChild]],
  reducer: Option[Reducer]
)

case class StepSlimChild(
  id: StepTreeId,
  mapper: Option[Mapper] = None
)

case class StepsFatDto(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType,
  action: Option[Execution],
  mapper: Option[Mapper] = None,
  reducer: Option[Reducer] = None,
  children: Option[List[StepsFatDto]]
)

case class StepsRootDto(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType
)


