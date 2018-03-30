package io.paradoxical.aetr.db

import io.paradoxical.aetr.core.db.dao.tables.{StepChildren, Steps}
import io.paradoxical.rdb.slick.providers.SlickDBProvider
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import io.paradoxical.common.extensions.Extensions._

class DbInitializer @Inject()(
  provider: SlickDBProvider,
  steps: Steps,
  stepChildren: StepChildren

)(implicit executionContext: ExecutionContext) {
  def init(): Unit = {
    List(steps, stepChildren).foreach { table =>
      provider.withDB(table.create).waitForResult()
    }
  }
}
