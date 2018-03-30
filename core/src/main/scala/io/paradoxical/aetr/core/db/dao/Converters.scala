package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.db.dao.tables.{StepChildrenDao, StepTreeDao}
import io.paradoxical.aetr.core.model._
import io.paradoxical.jackson.JacksonSerializer
import javax.inject.Inject

class Converters @Inject()(jacksonSerializer: JacksonSerializer) {
  def resolveSteps(bag: Seq[StepTreeDao], stepChildren: Seq[StepChildrenDao]): Seq[StepTree] = {
    def getChildren(id: StepTreeId): Seq[StepTreeDao] = {
      val children = stepChildren.filter(_.id == id).sortBy(_.childOrder).map(_.childId)

      bag.filter(item => children.contains(item.id))
    }

    def resolve(stepTreeDao: StepTreeDao): StepTree = {
      val childrenDaos = getChildren(stepTreeDao.id)

      lazy val childrenSteps = childrenDaos.map(resolve).toList

      stepTreeDao.stepType match {
        case StepType.Sequential =>
          SequentialParent(
            id = stepTreeDao.id,
            name = stepTreeDao.name,
            root = stepTreeDao.root,
            children = childrenSteps
          )
        case StepType.Parallel =>
          ParallelParent(
            id = stepTreeDao.id,
            name = stepTreeDao.name,
            root = stepTreeDao.root,
            children = childrenSteps
          )
        case StepType.Action =>
          Action(
            id = stepTreeDao.id,
            name = stepTreeDao.name,
            root = stepTreeDao.root,
            execution = stepTreeDao.execution.getOrElse(NoOp())
          )
      }
    }

    bag.map(resolve)
  }

  def run(runDao: RunDao, related: List[RunDao], rootTree: StepTree): Run = {
    ???
//    val repr: StepTree = ??? // find tree Id in tree
//
//    Run(
//      id = runDao.id,
//      root = runDao.root,
//      repr = repr,
//      state = runDao.state,
//      version = runDao.version,
//      result = runDao.result,
//      children = runDao.children.map(childId => {
//        val relatedDao = related.find(c => c.id == childId).get
//
//        run(relatedDao, related, trees)
//      })
//    )
  }
}
