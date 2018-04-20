package io.paradoxical.aetr.core.server.controllers.conversion

import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.controllers._
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object DtoConvertors {
  implicit class RichStepsDto(fatDto: StepsFatDto) {
    def toSlim: StepsSlimDto = {
      StepsSlimDto(
        id = fatDto.id,
        name = fatDto.name,
        action = fatDto.action,
        reducer = fatDto.reducer,
        children = fatDto.children.map(_.map(child => StepSlimChild(child.id, child.mapper))),
        stepType = fatDto.stepType
      )
    }
  }
}

class DtoConvertors @Inject()(stepDb: StepDb)(implicit executionContext: ExecutionContext) {
  def toRunResult(run: Run): RunTreeDto = {
    RunTreeDto(
      id = run.id,
      root = run.rootId,
      state = run.state,
      result = run.output,
      stepTree = StepsRootDto(
        id = run.repr.id,
        name = run.repr.name,
        stepType = stepType(run.repr)
      ),
      children = run.children.map(toRunResult)
    )
  }

  private def stepType(stepTree: StepTree): StepType = {
    stepTree match {
      case _: SequentialParent =>
        StepType.Sequential
      case _: ParallelParent =>
        StepType.Parallel
      case _: Action =>
        StepType.Action
    }
  }

  def fromStep(stepTree: StepTree): StepsFatDto = {
    val (action, reducer, children, typ) =
      stepTree match {
        case p: Parent =>
          val (typ, reducer) = p match {
            case _: SequentialParent =>
              (StepType.Sequential, None)
            case p: ParallelParent =>
              (StepType.Parallel, Some(p.reducer))
          }
          (None, reducer, Some(p.children.map(fromStep)), typ)
        case a: Action =>
          (Some(a.execution), None, None, StepType.Action)
      }

    StepsFatDto(
      id = stepTree.id,
      name = stepTree.name,
      action = action,
      children = children,
      stepType = typ,
      reducer = reducer,
      mapper = stepTree.mapper
    )
  }

  def toStep(step: StepsFatDto): StepTree = {
    step.action match {
      case Some(execution) =>
        Action(
          id = step.id,
          name = step.name,
          mapper = step.mapper,
          execution = execution
        )
      case None =>
        step.stepType match {
          case StepType.Sequential =>
            SequentialParent(
              id = step.id,
              name = step.name,
              children = step.children.getOrElse(Nil).map(toStep),
              mapper = step.mapper
            )
          case StepType.Parallel =>
            ParallelParent(
              id = step.id,
              name = step.name,
              mapper = step.mapper,
              reducer = step.reducer.getOrElse(Reducers.NoOp()),
              children = step.children.getOrElse(Nil).map(toStep)
            )
          case _ => ???
        }
    }
  }

  def toStep(stepDto: StepsSlimDto): Future[StepTree] = {
    stepDto.action match {
      case Some(execution) =>
        Future.successful(Action(
          id = stepDto.id,
          name = stepDto.name,
          execution = execution
        ))
      case None =>
        val dtoChildLookup = stepDto.children.getOrElse(Nil).map(c => c.id -> c).toMap

        stepDb.getSteps(dtoChildLookup.keys.toList).map(hydrated => {
          val children = stepDto.children.getOrElse(Nil)

          // hydrate the children from the database but then
          // populate the mapper we want from the input DTO
          val childrenWithMappers = children.map(child => {
            val hydratedChild = hydrated.find(_.id == child.id).get

            if (dtoChildLookup.contains(child.id)) {
              hydratedChild.withMapper(dtoChildLookup(child.id).mapper)
            } else {
              hydratedChild
            }
          })

          stepDto.stepType match {
            case StepType.Sequential =>
              SequentialParent(
                id = stepDto.id,
                name = stepDto.name,
                children = childrenWithMappers
              )
            case StepType.Parallel =>
              ParallelParent(
                id = stepDto.id,
                name = stepDto.name,
                children = childrenWithMappers,
                reducer = stepDto.reducer.getOrElse(Reducers.NoOp())
              )
            case StepType.Action => ???
          }
        })
    }
  }
}
