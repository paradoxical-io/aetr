package io.paradoxical.aetr.core.graph

import io.paradoxical.aetr.core.model
import io.paradoxical.aetr.core.model._
import java.util.UUID

class TreeManager(root: StepTree) {
  private lazy val rootId = RootId(UUID.randomUUID())

  def flatten: List[StepTree] = {
    def all0(curr: StepTree, acc: List[StepTree]): List[StepTree] = {
      curr match {
        case x: Parent =>
          if (x.children.isEmpty) {
            curr :: acc
          } else {
            curr :: x.children.flatMap(c => all0(c, acc))
          }
        case x: Action =>
          List(x)
      }
    }

    all0(root, Nil)
  }

  private def getChildren(node: StepTree): Seq[Run] = {
    node match {
      case p: Parent =>
        p.children.map(newRun0)
      case _: Action =>
        Nil
    }
  }

  def newRun(): Run = {
    val r = model.Run(
      RunInstanceId(rootId.value),
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
      RunInstanceId(UUID.randomUUID()),
      getChildren(tree),
      rootId,
      tree
    )
  }
}
