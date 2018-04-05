package io.paradoxical.aetr.core.server.controllers.conversion

import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.controllers.{StepsFatDto, StepsSlimDto}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object DtoConvertors {
  implicit class RichStepsDto(fatDto: StepsFatDto) {
    def toSlim: StepsSlimDto = {
      StepsSlimDto(
        id = fatDto.id,
        name = fatDto.name,
        action = fatDto.action,
        children = fatDto.children.map(_.map(_.id)),
        stepType = fatDto.stepType
      )
    }
  }
}

class DtoConvertors @Inject()(stepDb: StepDb)(implicit executionContext: ExecutionContext) {
  def fromStep(stepTree: StepTree): StepsFatDto = {
    val (action, children, typ) =
      stepTree match {
        case p: Parent =>
          val typ = p match {
            case _: SequentialParent =>
              StepType.Sequential
            case _: ParallelParent =>
              StepType.Parallel
          }
          (None, Some(p.children.map(fromStep)), typ)
        case a: Action =>
          (Some(a.execution), None, StepType.Action)
      }

    StepsFatDto(
      id = stepTree.id,
      name = stepTree.name,
      action = action,
      children = children,
      stepType = typ
    )
  }

  def toStep(step: StepsFatDto): StepTree = {
    step.action match {
      case Some(execution) =>
        Action(
          id = step.id,
          name = step.name,
          execution = execution
        )
      case None =>
        step.stepType match {
          case StepType.Sequential =>
            SequentialParent(
              id = step.id,
              name = step.name,
              children = step.children.getOrElse(Nil).map(toStep)
            )
          case StepType.Parallel =>
            ParallelParent(
              id = step.id,
              name = step.name,
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
        stepDb.getSteps(stepDto.children.getOrElse(Nil)).map(children => {
          stepDto.stepType match {
            case StepType.Sequential =>
              SequentialParent(
                id = stepDto.id,
                name = stepDto.name,
                children = children
              )
            case StepType.Parallel =>
              ParallelParent(
                id = stepDto.id,
                name = stepDto.name,
                children = children
              )
            case StepType.Action => ???
          }
        })
    }
  }
}
