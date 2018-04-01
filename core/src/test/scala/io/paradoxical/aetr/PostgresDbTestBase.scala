package io.paradoxical.aetr

import com.google.inject.Guice
import com.twitter.util.CountDownLatch
import io.paradoxical.aetr.core.db.Storage
import io.paradoxical.aetr.core.db.dao.{StepDb, VersionMismatchError}
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model._
import io.paradoxical.aetr.core.server.modules.ClockModule
import io.paradoxical.aetr.db.{DbInitializer, PostgresDbTestBase, TestModules}
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

    def complete(run: RootId): Unit = {
      val m = new RunManager(db.getRun(run).waitForResult())

      m.completeAll()

      db.upsertRun(m.root).waitForResult()
    }

    def pending(): List[Run] = {
      val runs = db.findRuns(RunState.Pending).flatMap(r => Future.sequence(r.map(db.getRun)))

      runs.waitForResult()
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
    val pulled = db.getRun(runRoot.rootId).waitForResult()

    // update it back should be ok since versions match
    db.upsertRun(pulled).waitForResult()
  }

  it should "lock on a row" in withDb { injector =>
    val leaf1: Action = Action(name = NodeName("leaf1"))

    val db = injector.instance[Storage]

    db.upsertSteps(leaf1)

    val run = new RunManager(leaf1).root

    assert(db.tryUpsertRun(run).isSuccess)

    def lockAndSet(state: RunState)(action: => Unit): Boolean = {
      db.tryLock(run.rootId)(r => {
        action

        db.trySetRunState(r.id, r.version, state)
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

    val db = injector.instance[Storage]

    db.upsertSteps(leaf1)

    val run = new RunManager(leaf1).root

    assert(db.tryUpsertRun(run).isSuccess)

    def lock(action: => Unit): Boolean = {
      db.tryLock(run.rootId)(r => {
        action

        true
      }).getOrElse(false)
    }

    assertThrows[Exception] {
      lock {
        throw new RuntimeException("Lock failed and was unset")
      }
    }

    // lock should be held still since it was not properly unlocked
    assert(!lock {})

    // move the clock fowrard but not enough to expire yet
    fakeClock.tick(testDbConfig.dbLockTime.minus(10 seconds))

    // lock should be held still since lock isn't expired
    assert(!lock {})

    // move the clock past the expiration date
    fakeClock.tick(testDbConfig.dbLockTime.plus(10 seconds))

    // lock should now be expired and so the lock can be acquired
    assert(lock {})

    // the lock was properly closed in the last run so new locks can be acquired
    assert(lock {})
  }
}