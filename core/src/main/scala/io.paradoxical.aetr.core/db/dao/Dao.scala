package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.model._
import io.paradoxical.jackson.JacksonSerializer

case class RunDao(
  id: RunId,
  children: Seq[RunId],
  root: RunId,
  parent: Option[RunId],
  stepTreeId: StepTreeId,
  state: StepState,
  result: Option[String]
)

case class StepTreeDao(
  id: StepTreeId,
  root: Option[StepTreeId],
  stepType: StepType,
  children: List[StepTreeId],
  configJson: Option[String]
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
          root = stepTreeDao.root,
          children = stepTreeDao.children.flatMap(materialize)
        )
      case StepType.Parallel =>
        ParallelParent(
          id = stepTreeDao.id,
          root = stepTreeDao.root,
          children = stepTreeDao.children.flatMap(materialize)
        )
      case StepType.Action =>
        Action(
          id = stepTreeDao.id,
          root = stepTreeDao.root,
          name = stepTreeDao.configJson.map(jacksonSerializer.fromJson[ActionData]).map(_.name).getOrElse("")
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
      result = runDao.result,
      children = runDao.children.map(childId => {
        val relatedDao = related.find(c => c.id == childId).get

        run(relatedDao, related, trees)
      })
    )
  }
}

case class ActionData(name: String)

