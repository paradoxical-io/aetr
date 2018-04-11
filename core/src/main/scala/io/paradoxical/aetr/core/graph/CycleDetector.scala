package io.paradoxical.aetr.core.graph

import io.paradoxical.aetr.core.model._

object CycleDetector {
  object Implicits {
    implicit class RichTree(stepTree: StepTree) {
      def hasCycles = new CycleDetector(stepTree).containsCycles()
    }
  }
}

class CycleDetector(tree: StepTree) {
  def containsCycles(next: StepTree = tree, path: Set[StepTreeId] = Set.empty): Boolean = {
    next match {
      case p: Parent =>
        if (path.contains(p.id)) {
          true
        } else {
          p.children.exists(child => containsCycles(child, path + p.id))
        }
      case _: Action =>
        false
    }
  }
}