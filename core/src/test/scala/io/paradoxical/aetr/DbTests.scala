package io.paradoxical.aetr

import com.google.inject.Guice
import com.twitter.util.CountDownLatch
import io.paradoxical.aetr.core.db.dao.{StepDb, VersionMismatchError}
import io.paradoxical.aetr.core.db.{DbInitializer, StepsDbSync}
import io.paradoxical.aetr.core.execution.ExecutionHandler
import io.paradoxical.aetr.core.execution.api.UrlExecutor
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.modules.ClockModule
import io.paradoxical.aetr.db.{PostgresDbTestBase, TestModules}
import io.paradoxical.aetr.mocks.TickClock
import io.paradoxical.common.extensions.Extensions._
import net.codingwell.scalaguice.InjectorExtensions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class DbTests extends PostgresDbTestBase {
  "DB" should "insert and update" in withDb { injector =>
    val db = injector.instance[StepDb]

    val leaf1 = Action(name = NodeName("leaf1"))
    val leaf2 = Action(name = NodeName("leaf2"))
    val parent = SequentialParent(name = NodeName("parent"), children = List(leaf1, leaf2))

    db.upsertStep(parent).waitForResult()

    db.getStep(parent.id).waitForResult() shouldEqual parent
  }

  it should "attach children and detach children" in withDb { injector =>
    val db = injector.instance[StepDb]

    val leaf1 = Action(name = NodeName("leaf1"))

    val parent = SequentialParent(name = NodeName("parent"))

    db.upsertStep(parent).waitForResult()

    db.upsertStep(leaf1).waitForResult()

    db.setChildren(parent.id, List(leaf1.id)).waitForResult()

    db.getStep(parent.id).waitForResult().asInstanceOf[SequentialParent].children.map(_.id) shouldEqual List(leaf1.id)

    db.setChildren(parent.id, Nil).waitForResult()

    db.getStep(parent.id).waitForResult().asInstanceOf[SequentialParent].children shouldEqual Nil
  }

  it should "save mappers and reducers" in withDb { injector =>
    val db = injector.instance[StepDb]

    val parent = ParallelParent(name = NodeName("leaf1"), mapper = Mappers.Identity(), reducer = Reducers.Last())

    db.upsertStep(parent).waitForResult()

    db.getStep(parent.id).waitForResult() shouldEqual parent
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

    db.getStep(parent.id).waitForResult() shouldEqual parentWithoutChild1
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

    db.getStep(root.id).waitForResult() shouldEqual root

    // drop a tree

    val rootMinusSubTree = root.copy(children = List(branch2, branch1))

    db.upsertStep(rootMinusSubTree).waitForResult()

    db.getStep(root.id).waitForResult() shouldEqual rootMinusSubTree
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

    val storedRoot = db.getRunTree(runRoot.rootId).waitForResult()

    val flattened = new RunManager(storedRoot).flatten

    flattened.map(_.run.id) shouldEqual new RunManager(runRoot).flatten.map(_.run.id)
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

    def complete(run: RootId): Unit = {
      val m = new RunManager(db.getRunTree(run).waitForResult())

      m.completeAll()

      db.upsertRun(m.root).waitForResult()

      assert(db.getRunTree(m.root.rootId).waitForResult().state == RunState.Complete)
    }

    def pending(): Seq[Run] = {
      val runs = db.findRuns(List(RunState.Pending), rootsOnly = true).flatMap(r => {
        Future.sequence(r.map(x => db.getRunTree(RootId(x.runDao.root.value))))
      })

      val x = runs.waitForResult()

      x.toList
    }

    db.upsertRun(mgr1.root).waitForResult()
    db.upsertRun(mgr2.root).waitForResult()

    pending().map(_.id) shouldEqual List(mgr1.root.id, mgr2.root.id)

    complete(mgr2.root.rootId)

    pending().map(_.id) shouldEqual List(mgr1.root.id)

    complete(mgr1.root.rootId)

    pending().map(_.id) shouldEqual Nil
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
    val pulled = db.getRunTree(runRoot.rootId).waitForResult()

    // update it back should be ok since versions match
    db.upsertRun(pulled).waitForResult()
  }

  it should "lock on a row" in withDb { injector =>
    val leaf1: Action = Action(name = NodeName("leaf1"))

    val db = injector.instance[StepsDbSync]

    db.upsertSteps(leaf1)

    val run = new RunManager(leaf1).root

    assert(db.tryUpsertRun(run).isSuccess)

    def lockAndSet(state: RunState)(action: => Unit): Boolean = {
      db.tryLock(run.rootId)(r => {
        action

        db.trySetRunState(r.id, db.getRunTree(r.rootId), state)
      }).getOrElse(false)
    }

    val threadTryAcquire = new CountDownLatch(1)

    val threadRunning = new CountDownLatch(1)

    var threadSet = false

    val threadTriedtoAcquire = new CountDownLatch(1)

    val t = new Thread(() => {
      threadRunning.countDown()

      threadTryAcquire.await()

      // should not be able to set this
      threadSet = lockAndSet(RunState.Executing) {}

      threadTriedtoAcquire.countDown()
    })

    t.start()

    // make sure the thread is booted up
    threadRunning.await()

    val set = lockAndSet(RunState.Complete) {
      // allow the thread to try and process
      threadTryAcquire.countDown()

      // wait for the thread to try and acquire the lock we are currently in
      threadTriedtoAcquire.await()
    }

    t.join()

    // only allow one of the items to bet set, either the async thread
    // or the current one
    assert(set)

    // the thread tried to acquire a lock but was unable to
    // since the local thread was in the lock
    assert(!threadSet)
  }

  it should "expire locked rows" in {
    import TestModules._

    val testDbConfig = newDbAndConfig

    val fakeClock = new TickClock

    val modules = TestModules(testDbConfig).overlay(new ClockModule(fakeClock))

    val injector = Guice.createInjector(modules: _*)

    val leaf1: Action = Action(name = NodeName("leaf1"))

    injector.instance[DbInitializer].init()

    val db = injector.instance[StepsDbSync]

    db.upsertSteps(leaf1)

    val run = new RunManager(leaf1).root

    assert(db.tryUpsertRun(run).isSuccess)

    def lock(action: => Unit): Boolean = {
      db.tryLock(run.rootId)(r => {
        action

        true
      }).getOrElse(false)
    }

    assert(db.findUnlockedRuns(RunState.Pending).size == 1)

    assertThrows[Exception] {
      lock {
        throw new RuntimeException("Lock failed and was unset")
      }
    }

    assert(db.findUnlockedRuns(RunState.Pending).size == 0)

    // lock should be held still since it was not properly unlocked
    assert(!lock {})
    assert(db.findUnlockedRuns(RunState.Pending).size == 0)

    // move the clock fowrard but not enough to expire yet
    fakeClock.tick(testDbConfig.dbLockTime.minus(10 seconds))

    // lock should be held still since lock isn't expired
    assert(!lock {})
    assert(db.findUnlockedRuns(RunState.Pending).size == 0)

    // move the clock past the expiration date
    fakeClock.tick(testDbConfig.dbLockTime.plus(10 seconds))

    // lock should now be expired and so the lock can be acquired
    assert(lock {})
    assert(db.findUnlockedRuns(RunState.Pending).size == 1)

    // the lock was properly closed in the last run so new locks can be acquired
    assert(lock {})
    assert(db.findUnlockedRuns(RunState.Pending).size == 1)
  }

  "Execution handler" should "respect run version in setting complete" in withDb { injector =>
    val db = injector.instance[StepsDbSync]

    val urlExecutor = mock[UrlExecutor]

    val tree = Action(name = NodeName("test"), execution = NoOp())

    val run = new RunManager(tree).root

    db.upsertSteps(tree)
    db.tryUpsertRun(run)

    // make su re a no-op setting itself to complete doesn't re-set itself
    // back to executing
    new ExecutionHandler(db, urlExecutor).execute(
      Actionable(db.loadRun(run.rootId), tree, None)
    )

    db.loadRun(run.rootId).state shouldEqual RunState.Complete
  }
}