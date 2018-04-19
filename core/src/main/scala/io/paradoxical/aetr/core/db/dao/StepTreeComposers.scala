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
    def getChildren(id: StepTreeId): Seq[(StepTreeDao, StepChildrenDao)] = {
      val children = stepChildren.filter(_.id == id).sortBy(_.childOrder)

      children.flatMap(x => {
        val foundChild = bag.find(_.id == x.childId)

        foundChild.map(c => (c, x))
      })
    }

    def resolve(stepTreeDao: StepTreeDao): StepTree = {
      val childList = getChildren(stepTreeDao.id)

      val childLookup = childList.map(x => (x._1.id, x._2.childOrder) -> x).toMap

      lazy val childrenSteps = childList.map(c => resolve(c._1)).zipWithIndex.map {
        case (child, index) =>
          // reset the mapper given the childs order from the db
          val mapper = childLookup((child.id, index))._2.mapper

          child.withMapper(mapper)
      }.toList

      stepTreeDao.stepType match {
        case StepType.Sequential =>
          SequentialParent(
            id = stepTreeDao.id,
            name = stepTreeDao.name,
            children = childrenSteps
          )
        case StepType.Parallel =>
          ParallelParent(
            id = stepTreeDao.id,
            name = stepTreeDao.name,
            reducer = stepTreeDao.reducer.getOrElse(Reducers.NoOp()),
            children = childrenSteps
          )
        case StepType.Action =>
          Action(
            id = stepTreeDao.id,
            name = stepTreeDao.name,
            execution = stepTreeDao.execution.getOrElse(NoOp())
          )
      }
    }

    bag.map(resolve)
  }
}

case class StepChildWithMapper(id: StepTreeId, mapper: Option[Mapper])

object StepTreeComposer {
  def childrenToDao(parent: StepTreeId, children: List[StepChildWithMapper]): Seq[StepChildrenDao] = {
    children.zipWithIndex.map {
      case (child, order) =>
        StepChildrenDao(
          id = parent,
          childOrder = order,
          childId = child.id,
          child.mapper
        )
    }
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
          StepTreeComposer.childrenToDao(p.id, p.children.map(child => StepChildWithMapper(child.id, child.mapper)))
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
              reducer = None,
              execution = None
            )
          case p: ParallelParent =>
            StepTreeDao(
              id = p.id,
              name = p.name,
              stepType = StepType.Parallel,
              reducer = Some(p.reducer),
              execution = None
            )
        }
      case p: Action =>
        StepTreeDao(
          id = p.id,
          name = p.name,
          stepType = StepType.Action,
          execution = Some(p.execution),
          reducer = None
        )
    }
  }
}
