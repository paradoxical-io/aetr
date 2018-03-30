package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.model._
import javax.inject.Inject

class Converters @Inject()() {
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
