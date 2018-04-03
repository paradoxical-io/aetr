package io.paradoxical.aetr.core.server.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.exceptions.ConflictException
import com.twitter.finatra.request.{QueryParam, RouteParam}
import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.execution.{AdvanceQueuer, Completor, RunToken}
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.controllers.conversion.DtoConvertors
import io.paradoxical.finatra.Framework
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future, blocking}

class RunsController @Inject()(
  db: StepDb,
  advanceQueuer: AdvanceQueuer,
  completor: Completor,
  converters: DtoConvertors
)(implicit executionContext: ExecutionContext) extends Framework.RestApi {
  postWithDoc("/api/v1/runs/step/:id") {
    _.description("Start a new run from a step").request[CreateRunRequest].responseWith[CreateRunResult](status = 200)
  } { r: CreateRunRequest =>
    db.getStep(r.id).
      map(stepTree => new RunManager(stepTree).root).
      flatMap(r => {
        db.upsertRun(r, r.input).map(instanceId => {
          advanceQueuer.enqueue(r.rootId)

          instanceId
        })
      }).
      map(CreateRunResult)
  }

  getWithDoc("/api/v1/runs/partial/:id") {
    _.description("Get raw run id state. Only tracks the state of this instance,  not representative of the entire tree").request[GetRunRequest].responseWith[GetRunResult](status = 200)
  } { r: GetRunRequest =>
    db.getRun(RunInstanceId(r.id)).map(dao => GetRunResult(state = dao.state, result = dao.output, dao.stepTreeId))
  }

  getWithDoc("/api/v1/runs/:id") {
    _.description("Get state of the run tree").request[GetRunRequest].responseWith[GetRunResult](status = 200)
  } { r: GetRunRequest =>
    db.getRunTree(RootId(r.id)).map(dao => GetRunResult(state = dao.state, result = dao.output, dao.repr.id))
  }

  getWithDoc("/api/v1/runs/active") {
    _.description("Get active runs").responseWith[List[GetRunDataResult]](status = 200)
  } { r: Request =>
    db.findRuns(List(RunState.Executing, RunState.Pending)).map(_.map(x =>
      GetRunDataResult(
        x.runDao.id,
        RootId(x.runDao.root.value),
        x.runDao.state,
        x.runDao.stepTreeId,
        x.stepTreeDao.name,
        x.runDao.output
      )))
  }

  getWithDoc("/api/v1/runs/state") {
    _.description("Get runs in a state").request[GetRunsByStateRequest].responseWith[List[GetRunDataResult]](status = 200)
  } { r: GetRunsByStateRequest =>
    db.findRuns(r.state).map(_.map(x =>
      GetRunDataResult(
        x.runDao.id,
        RootId(x.runDao.root.value),
        x.runDao.state,
        x.runDao.stepTreeId,
        x.stepTreeDao.name,
        x.runDao.output
      )))
  }

  getWithDoc("/api/v1/runs/related/step/:id") {
    _.description("Get runs related to a step").request[GetRelatedRunsRequest].responseWith[GetRelatedRunsResult](status = 200)
  } { r: GetRelatedRunsRequest =>
    db.findRelatedRuns(r.id).
      map(_.map(dao => GetRunResult(state = dao.state, result = dao.output, dao.stepTreeId))).
      map(GetRelatedRunsResult)
  }

  postWithDoc("/api/v1/runs/complete") {
    _.description("Complete a run").request[CompleteRunRequest].responseWith[Unit](status = 200)
  } { req: CompleteRunRequest =>
    val token = RunToken(req.token)

    Future {
      blocking {
        completor.complete(token, req.result)
      }
    }.map(completed =>
      if (!completed) {
        throw ConflictException("Token is already completed")
      })
  }
}

case class CreateRunRequest(@RouteParam id: StepTreeId, input: Option[ResultData])
case class CreateRunResult(id: RunInstanceId)

case class GetRunRequest(@RouteParam id: UUID)

case class GetRunResult(state: RunState, result: Option[ResultData], stepTreeId: StepTreeId)

case class CompleteRunRequest(@QueryParam token: String, result: Option[ResultData])

case class GetRunDataResult(
  id: RunInstanceId,
  root: RootId,
  state: RunState,
  stepTreeId: StepTreeId,
  nodeName: NodeName,
  result: Option[ResultData]
) {
  val token = RunToken(id, root).asRaw
}

case class GetRelatedRunsRequest(@RouteParam id: StepTreeId)
case class GetRelatedRunsResult(runs: Seq[GetRunResult])

case class GetRunsByStateRequest(@QueryParam state: List[RunState])