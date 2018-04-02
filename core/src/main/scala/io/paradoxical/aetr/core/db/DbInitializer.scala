package io.paradoxical.aetr.core.db

import io.paradoxical.aetr.core.db.dao.tables.{Runs, StepChildren, Steps}
import io.paradoxical.common.extensions.Extensions._
import io.paradoxical.rdb.slick.providers.SlickDBProvider
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DbInitializer @Inject()(
  provider: SlickDBProvider,
  steps: Steps,
  stepChildren: StepChildren,
  runs: Runs

)(implicit executionContext: ExecutionContext) {
  def init(): Unit = {
    List(steps, stepChildren, runs).foreach { table =>
      provider.withDB(table.create).waitForResult()
    }
  }
}
