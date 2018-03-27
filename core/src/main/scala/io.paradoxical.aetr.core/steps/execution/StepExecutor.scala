package io.paradoxical.aetr.core.steps.execution

import io.paradoxical.aetr.core.steps.StepState
import io.paradoxical.aetr.core.task._
import java.net.URL
import javax.inject.Inject

trait UrlExecutor {
  def call(url: URL, stepInstanceId: StepInstanceId, data: Option[StepInstanceResult]): Unit
}

class StepExecutor @Inject()(stepLoader: StepLoader, urlExecutor: UrlExecutor) extends StepProcessor {
  override def execute(step: Step, data: Option[StepInstanceResult]): StepInstance = {
    step match {
      case x: ApiStep =>
        val instance = StepInstance.newInstance(step).copy(
          nextStep = x.next,
          source = step.id
        )

        stepLoader.upsertStepInstance(instance.copy(state = StepState.Pending))

        urlExecutor.call(x.url, instance.id, data)

        stepLoader.upsertStepInstance(instance.copy(state = StepState.Running))
    }
  }

  override def advance(from: StepInstanceId, result: Option[StepInstanceResult]): List[StepInstance] = {
    val instanceO = stepLoader.getStepInstance(from)

    require(instanceO.isDefined)

    val instance = instanceO.get.copy(result = result, state = StepState.ExecutingNext)

    stepLoader.upsertStepInstance(instance)

    val nextInstances =
      instance.nextStep.map(n => next(n, result)).
        getOrElse(Nil).
        map(_.copy(previous = Some(instance.id))).
        map(stepLoader.upsertStepInstance)

    stepLoader.upsertStepInstance(instance.copy(state = StepState.Complete))

    nextInstances
  }

  private def next(subSteps: SubSteps, data: Option[StepInstanceResult]): List[StepInstance] = {
    subSteps match {
      case SequentialSubSteps(next) =>
        val toExecute = next.headOption

        if (toExecute.isEmpty) {
          Nil
        } else {
          val toDefer = next.tail

          val remaining = SequentialSubSteps(toDefer)

          val step = toExecute.get.withNext(remaining)

          List(execute(step, data))
        }
      case ParallelSubSteps(next) =>
        next.par.map(execute(_, data)).toList
    }
  }
}
