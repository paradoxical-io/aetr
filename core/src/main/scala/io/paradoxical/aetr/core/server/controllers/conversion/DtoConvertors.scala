package io.paradoxical.aetr.core.server.controllers.conversion

import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.controllers.StepDto

class DtoConvertors {
  def fromStep(stepTree: StepTree): StepDto = {
    val (action, children, typ) =
      stepTree match {
        case p: Parent =>
          val typ = p match {
            case _: SequentialParent =>
              StepType.Sequential
            case _: ParallelParent =>
              StepType.Parallel
          }
          (None, p.children.map(fromStep), typ)
        case a: Action =>
          (Some(a.execution), Nil, StepType.Action)
      }

    StepDto(
      root = stepTree.root,
      id = stepTree.id,
      name = stepTree.name,
      action = action,
      children = children,
      stepType = typ
    )
  }

  def toStep(stepDto: StepDto): StepTree = {
    stepDto.action match {
      case Some(execution) =>
        Action(
          id = stepDto.id,
          name = stepDto.name,
          execution = execution,
          root = stepDto.root
        )
      case None =>
        stepDto.stepType match {
          case StepType.Sequential =>
            SequentialParent(
              id = stepDto.id,
              name = stepDto.name,
              root = stepDto.root,
              children = stepDto.children.map(toStep)
            )
          case StepType.Parallel =>
            ParallelParent(
              id = stepDto.id,
              name = stepDto.name,
              root = stepDto.root,
              children = stepDto.children.map(toStep)
            )
          case StepType.Action => ???
        }
    }
  }
}
