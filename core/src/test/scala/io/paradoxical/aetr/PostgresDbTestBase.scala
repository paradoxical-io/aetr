package io.paradoxical.aetr

import io.paradoxical.aetr.core.db.dao.{StepDb, VersionMismatchError}
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.db.PostgresDbTestBase
import io.paradoxical.common.extensions.Extensions._
import net.codingwell.scalaguice.InjectorExtensions._

class DbTests extends PostgresDbTestBase {
  "DB" should "insert and update" in withDb { injector =>
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

  it should "work on nested trees" in withDb { injector =>
    val db = injector.instance[StepDb]

    val leaf1: Action = Action(name = NodeName("leaf1"))

    val leaf2 = Action(name = NodeName("leaf2"))

    val branch1 = SequentialParent(name = NodeName("branch1"), children = List(leaf1, leaf2))

    val branch2 = SequentialParent(name = NodeName("branch2"), children = List(leaf1, leaf2))

    val branch3 = ParallelParent(name = NodeName("parallel"), children = List(leaf1, leaf2))

    val root = SequentialParent(name = NodeName("root"), children = List(branch3, branch2, branch1))

    db.upsertStep(root).waitForResult()

    db.getTree(root.id).waitForResult() shouldEqual root

    // drop a tree

    val rootMinusSubTree = root.copy(children = List(branch2, branch1))

    db.upsertStep(rootMinusSubTree).waitForResult()

    db.getTree(root.id).waitForResult() shouldEqual rootMinusSubTree
  }

  it should "upsert and retrieve runs" in withDb { injector =>
    val db = injector.instance[StepDb]

    val leaf1: Action = Action(name = NodeName("leaf1"))

    val leaf2 = Action(name = NodeName("leaf2"))

    val branch1 = SequentialParent(name = NodeName("branch1"), children = List(leaf1, leaf2))

    val branch2 = SequentialParent(name = NodeName("branch2"), children = List(leaf1, leaf2))

    val branch3 = ParallelParent(name = NodeName("parallel"), children = List(leaf1, leaf2))

    val stepRoot = SequentialParent(name = NodeName("root"), children = List(branch3, branch2, branch1))

    db.upsertStep(stepRoot).waitForResult()

    val runRoot = new RunManager(stepRoot).root

    db.upsertRun(runRoot).waitForResult()

    val storedRoot = db.getRun(runRoot.rootId).waitForResult()

    new RunManager(storedRoot).flatten.map(_.id) shouldEqual new RunManager(runRoot).flatten.map(_.id)
  }

  it should "list pending runs" in withDb { injector =>
    val db = injector.instance[StepDb]

    val leaf1: Action = Action(name = NodeName("leaf1"))

    val leaf2 = Action(name = NodeName("leaf2"))

    val branch1 = SequentialParent(name = NodeName("branch1"), children = List(leaf1, leaf2))

    val branch2 = SequentialParent(name = NodeName("branch2"), children = List(leaf1, leaf2))

    val branch3 = ParallelParent(name = NodeName("parallel"), children = List(leaf1, leaf2))

    val stepRoot = SequentialParent(name = NodeName("root"), children = List(branch3, branch2, branch1))

    db.upsertStep(stepRoot).waitForResult()

    val mgr1 = new RunManager(stepRoot)
    val mgr2 = new RunManager(stepRoot)

    def complete(run: Root): Unit = {
      val m = new RunManager(db.getRun(run).waitForResult())

      m.completeAll()

      db.upsertRun(m.root).waitForResult()
    }

    db.upsertRun(mgr1.root).waitForResult()
    db.upsertRun(mgr2.root).waitForResult()

    db.getPendingRuns().waitForResult().map(_.id) shouldEqual List(mgr1.root.id, mgr2.root.id)

    complete(mgr2.root.rootId)

    db.getPendingRuns().waitForResult().map(_.id) shouldEqual List(mgr1.root.id)

    complete(mgr1.root.rootId)

    db.getPendingRuns().waitForResult().map(_.id) shouldEqual Nil
  }

  it should "fail to upsert a run if the version is invalid" in withDb { injector =>
    val db = injector.instance[StepDb]

    val leaf1: Action = Action(name = NodeName("leaf1"))

    val leaf2 = Action(name = NodeName("leaf2"))

    val branch1 = SequentialParent(name = NodeName("branch1"), children = List(leaf1, leaf2))

    val branch2 = SequentialParent(name = NodeName("branch2"), children = List(leaf1, leaf2))

    val branch3 = ParallelParent(name = NodeName("parallel"), children = List(leaf1, leaf2))

    val stepRoot = SequentialParent(name = NodeName("root"), children = List(branch3, branch2, branch1))

    db.upsertStep(stepRoot).waitForResult()

    val runRoot = new RunManager(stepRoot).root

    // node doesn't exist so it should insert
    db.upsertRun(runRoot).waitForResult()

    // didn't request the new run so its out of date now
    assertThrows[VersionMismatchError] {
      db.upsertRun(runRoot).waitForResult()
    }

    // pull from the db to get the latest version
    val pulled = db.getRun(runRoot.rootId).waitForResult()

    // update it back should be ok since versions match
    db.upsertRun(pulled).waitForResult()
  }
}