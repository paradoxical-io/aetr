package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.model._
import io.paradoxical.jackson.JacksonSerializer
import java.time.Instant
import javax.inject.Inject

case class RunDao(
  id: RunId,
  children: Seq[RunId],
  root: Root,
  parent: Option[RunId],
  version: Version,
  stepTreeId: StepTreeId,
  state: StepState,
  result: Option[ResultData],
  createdAt: Instant,
  lastUpdatedAt: Instant,
  stateUpdatedAt: Instant
)

/** *
 *
 * @param id
 * @param name
 * @param root
 * @param stepType
 * @param children
 * @param roots Related tree roots to query for when slurping. These are trees that are linked to
 *              in children and need to be materialized in the related set list
 * @param executionJson
 * @param createdAt
 * @param lastUpdatedAt
 */
case class StepTreeDao(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType,
  children: List[StepTreeId],
  root: Option[StepTreeId],
  roots: List[StepTreeId],
  executionJson: String,
  createdAt: Instant,
  lastUpdatedAt: Instant
)

class Converters @Inject()(jacksonSerializer: JacksonSerializer) {
  def step(stepTreeDao: StepTreeDao, related: List[StepTreeDao]): StepTree = {
    def materialize(id: StepTreeId): Option[StepTree] = {
      related.find(_.id == id).map(r => step(r, related))
    }

    stepTreeDao.stepType match {
      case StepType.Sequential =>
        SequentialParent(
          id = stepTreeDao.id,
          name = stepTreeDao.name,
          root = stepTreeDao.root,
          children = stepTreeDao.children.flatMap(materialize)
        )
      case StepType.Parallel =>
        ParallelParent(
          id = stepTreeDao.id,
          name = stepTreeDao.name,
          root = stepTreeDao.root,
          children = stepTreeDao.children.flatMap(materialize)
        )
      case StepType.Action =>
        Action(
          id = stepTreeDao.id,
          name = stepTreeDao.name,
          root = stepTreeDao.root,
          execution = jacksonSerializer.fromJson[Execution](stepTreeDao.executionJson)
        )
    }
  }

  def run(runDao: RunDao, related: List[RunDao], trees: List[StepTreeDao]): Run = {
    val repr: StepTree = trees.find(_.id == runDao.stepTreeId).map(t => step(t, trees)).get

    Run(
      id = runDao.id,
      root = runDao.root,
      repr = repr,
      state = runDao.state,
      version = runDao.version,
      result = runDao.result,
      children = runDao.children.map(childId => {
        val relatedDao = related.find(c => c.id == childId).get

        run(relatedDao, related, trees)
      })
    )
  }
}