package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.db.dao.tables.{StepChildrenDao, StepTreeDao}
import io.paradoxical.aetr.core.graph.TreeManager
import io.paradoxical.aetr.core.model._

class StepTreeComposer {
  /**
   * Takes a bag of tree items and a set of children
   * and rebuilds a set of step trees
   *
   * @param bag
   * @param stepChildren
   * @return
   */
  def reconstitute(bag: Seq[StepTreeDao], stepChildren: Seq[StepChildrenDao]): Seq[StepTree] = {
    def getChildren(id: StepTreeId): Seq[StepTreeDao] = {
      val children = stepChildren.filter(_.id == id).sortBy(_.childOrder).map(_.childId)

      children.flatMap(x => bag.find(_.id == x))
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
}

/**
 * Takes a step tree and creates a flattened list of daos and
 * child daos to save
 *
 * @param stepTree
 */
class StepTreeDecomposer(stepTree: StepTree) {
  private val flattened = new TreeManager(stepTree).flatten

  val dao: Seq[StepTreeDao] = flattened.map(toDao)

  val children: Seq[StepChildrenDao] = {
    flattened.flatMap(item => {
      item match {
        case p: Parent =>
          p.children.zipWithIndex.map {
            case (child, order) =>
              StepChildrenDao(
                id = p.id,
                childOrder = order,
                childId = child.id
              )
          }
        case _: Action =>
          Nil
      }
    })
  }

  // TODO: store mapper/reducer
  private def toDao(stepTree: StepTree): StepTreeDao = {
    stepTree match {
      case x: Parent =>
        x match {
          case p: SequentialParent =>
            StepTreeDao(
              id = p.id,
              name = p.name,
              stepType = StepType.Sequential,
              root = p.root,
              execution = None
            )
          case p: ParallelParent =>
            StepTreeDao(
              id = p.id,
              name = p.name,
              stepType = StepType.Parallel,
              root = p.root,
              execution = None
            )
        }
      case p: Action =>
        StepTreeDao(
          id = p.id,
          name = p.name,
          stepType = StepType.Action,
          root = p.root,
          execution = Some(p.execution)
        )
    }
  }
}
