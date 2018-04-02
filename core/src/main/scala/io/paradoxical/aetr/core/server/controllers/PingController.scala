package io.paradoxical.aetr.core.server.controllers

import com.twitter.finagle.http.Request
import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.controllers.conversion.DtoConvertors
import io.paradoxical.finatra.Framework
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PingController extends Framework.RestApi {
  getWithDoc("/ping") {
    _.description("Ping API")
  } { _: Request =>
    "pong"
  }
}

class StepsController @Inject()(db: StepDb)(implicit executionContext: ExecutionContext) extends Framework.RestApi {
  getWithDoc("/api/v1/steps/:id") {
    _.description("Get steps").request[GetStepsId]
  } { r: GetStepsId =>
    db.getStep(r.id).map(new DtoConvertors().fromStep)
  }

  postWithDoc("/api/v1/steps") {
    _.description("Post steps").request[StepDto]
  } { r: StepDto =>
    val t = new DtoConvertors().toStep(r)

    db.upsertStep(t)
  }
}

case class GetStepsId(id: StepTreeId)

case class StepDto(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType,
  root: Option[StepTreeId],
  action: Option[Execution],
  children: List[StepDto]
)