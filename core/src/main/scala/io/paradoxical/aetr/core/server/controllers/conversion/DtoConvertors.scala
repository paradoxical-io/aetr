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
        root = fatDto.root,
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
      root = stepTree.root,
      id = stepTree.id,
      name = stepTree.name,
      action = action,
      children = children,
      stepType = typ
    )
  }

  def toStep(stepDto: StepsSlimDto): Future[StepTree] = {
    stepDto.action match {
      case Some(execution) =>
        Future.successful(Action(
          id = stepDto.id,
          name = stepDto.name,
          execution = execution,
          root = stepDto.root
        ))
      case None =>
        stepDb.getSteps(stepDto.children.getOrElse(Nil)).map(children => {
          stepDto.stepType match {
            case StepType.Sequential =>
              SequentialParent(
                id = stepDto.id,
                name = stepDto.name,
                root = stepDto.root,
                children = children
              )
            case StepType.Parallel =>
              ParallelParent(
                id = stepDto.id,
                name = stepDto.name,
                root = stepDto.root,
                children = children
              )
            case StepType.Action => ???
          }
        })
    }
  }
}
