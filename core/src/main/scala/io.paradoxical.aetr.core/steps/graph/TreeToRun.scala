package io.paradoxical.aetr.core.steps.graph

import io.paradoxical.aetr.core.model
import io.paradoxical.aetr.core.model._
import java.util.UUID

class TreeToRun(root: StepTree) {
  lazy val run: Run = newRun()

  private lazy val rootId = RunId(UUID.randomUUID())

  private def getChildren(node: StepTree): Seq[Run] = {
    node match {
      case p: Parent =>
        p.children.map(newRun0)
      case _: Action =>
        Nil
    }
  }

  private def newRun(): Run = {
    val r = model.Run(
      rootId,
      getChildren(root),
      rootId,
      root
    )

    setParents(r)

    r
  }

  private def setParents(parent: Run): Unit = {
    parent.children.foreach(r => {
      r.parent = Some(parent)
      setParents(r)
    })
  }

  private def newRun0(tree: StepTree): Run = {
    model.Run(
      RunId(UUID.randomUUID()),
      getChildren(tree),
      rootId,
      tree
    )
  }
}
