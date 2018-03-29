package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.model._
import io.paradoxical.jackson.JacksonSerializer
import java.time.Instant

case class RunDao(
  id: RunId,
  children: Seq[RunId],
  root: Root,
  parent: Option[RunId],
  version: Version,
  stepTreeId: StepTreeId,
  state: StepState,
  result: Option[String],
  createdAt: Instant,
  lastUpdatedAt: Instant
)

case class StepTreeDao(
  id: StepTreeId,
  name: String,
  root: Option[StepTreeId],
  stepType: StepType,
  children: List[StepTreeId],
  executionJson: String,
  createdAt: Instant,
  lastUpdatedAt: Instant
)

class Converters(jacksonSerializer: JacksonSerializer) {
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