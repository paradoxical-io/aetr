//package io.paradoxical.aetr.core.steps.execution
//
//import io.paradoxical.aetr.core.steps.StepState
//import io.paradoxical.aetr.core.task._
//import java.net.URL
//import javax.inject.Inject
//
//trait UrlExecutor {
//  def call(url: URL, stepInstanceId: StepInstanceId, data: Option[StepInstanceResult]): Unit
//}
//
//
//
//object Topology {
//  def create(root: Step): SubSteps = {
//    if (root.next.isEmpty) {
//      return SequentialSubSteps(Nil)
//    }
//
//    root.next.get match {
//      case x @ SequentialSubSteps(next) =>
//        LinkedSubSteps(next.map(create))
//      case x @ ParallelSubSteps(next) =>
//        LinkedSubSteps(next.map(create))
//      case x @ LinkedSubSteps(subSteps) =>
//        LinkedSubSteps(x :: subSteps)
//    }
//  }
//}
//
//class StepExecutor @Inject()(stepLoader: StepLoader, urlExecutor: UrlExecutor) extends StepProcessor {
//  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)
//
//  override def execute(step: Step, data: Option[StepInstanceResult]): StepInstance = {
//    step match {
//      case x: ApiStep =>
//        val instance = StepInstance.newInstance(step).copy(
//          nextStep = x.next,
//          source = step.id
//        )
//
//        logger.info(s"Executing ${step.name}")
//
//        stepLoader.upsertStepInstance(instance.copy(state = StepState.Pending))
//
//        urlExecutor.call(x.url, instance.id, data)
//
//        stepLoader.upsertStepInstance(instance.copy(state = StepState.Running))
//    }
//  }
//
//  override def advance(from: StepInstanceId, result: Option[StepInstanceResult]): List[StepInstance] = {
////    val instanceO = stepLoader.getStepInstance(from)
////
////    logger.info(s"Advancing ${stepLoader.getStep(instanceO.get.source).get.name}")
////
////    require(instanceO.isDefined)
////
////    val instance = instanceO.get.copy(result = result, state = StepState.ExecutingNext)
////
////    stepLoader.upsertStepInstance(instance)
////
////    val nextInstances =
////      instance.nextStep.map(n => next(n, result)).
////        getOrElse(Nil).
////        map(_.copy(previous = Some(instance.id))).
////        map(stepLoader.upsertStepInstance)
////
////    stepLoader.upsertStepInstance(instance.copy(state = StepState.Complete))
////
////    nextInstances
//    ???
//  }
//
////  private def next(subSteps: SubSteps, data: Option[StepInstanceResult], trailingSteps: Option[SubSteps]): List[StepInstance] = {
////    subSteps match {
////      case SequentialSubSteps(next) =>
////        val toExecute = next.headOption
////
////        if (toExecute.isEmpty) {
////          Nil
////        } else {
////          val toDefer = next.tail
////
////          val nestedSteps = toExecute.flatMap(_.next)
////
////          val remaining = SequentialSubSteps(toDefer)
////
////          val combined =
////            if (nestedSteps.isDefined) {
////              LinkedSubSteps(List(nestedSteps.get, remaining))
////            } else {
////              remaining
////            }
////
////          val step = toExecute.get.withNext(combined)
////
////          List(execute(step, data))
////        }
////      case LinkedSubSteps(chained) =>
////        val toExecute: Option[SubSteps] = chained.headOption
////
////        if (toExecute.isEmpty) {
////          Nil
////        } else {
////          val toDefer = chained.tail
////
////          val remaining = if (toDefer.nonEmpty) Some(LinkedSubSteps(toDefer)) else None
////
////          next(toExecute.get, data, remaining)
////        }
////      case ParallelSubSteps(next) =>
////        next.par.map(execute(_, data)).toList
////    }
////  }
//}
