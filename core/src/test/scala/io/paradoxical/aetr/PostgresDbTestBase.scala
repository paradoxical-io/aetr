package io.paradoxical.aetr

import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.db.PostgresDbTestBase
import io.paradoxical.common.extensions.Extensions._
import net.codingwell.scalaguice.InjectorExtensions._

class DbTests extends PostgresDbTestBase {
  "DB" should "create" in withDb { injector =>
    val db = injector.instance[StepDb]

    val leaf1 = Action(name = NodeName("leaf1"))
    val leaf2 = Action(name = NodeName("leaf2"))
    val parent = SequentialParent(name = NodeName("parent"), children = List(leaf1, leaf2))

    db.upsertStep(parent).waitForResult()

    db.getTree(parent.id).waitForResult() shouldEqual parent
  }

  it should "rebuild children on change" in withDb { injector =>
    val db = injector.instance[StepDb]

    val leaf1 = Action(name = NodeName("leaf1"))
    val leaf2 = Action(name = NodeName("leaf2"))
    val leaf3 = Action(name = NodeName("leaf3"))
    val parent = SequentialParent(name = NodeName("parent"), children = List(leaf1, leaf2))

    db.upsertStep(parent).waitForResult()

    // change the order and add a new one
    val parentWithoutChild1 = parent.copy(children = List(leaf2, leaf1, leaf3))

    db.upsertStep(parentWithoutChild1).waitForResult()

    db.getTree(parent.id).waitForResult() shouldEqual parentWithoutChild1
  }
}