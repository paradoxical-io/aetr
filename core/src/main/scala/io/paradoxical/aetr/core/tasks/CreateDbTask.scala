package io.paradoxical.aetr.core.tasks

import com.google.inject.Guice
import io.paradoxical.aetr.core.db.DbInitializer
import io.paradoxical.aetr.core.server.modules.Modules
import io.paradoxical.tasks.{Task, TaskDefinition}
import net.codingwell.scalaguice.InjectorExtensions._
import scala.concurrent.ExecutionContext.Implicits.global

class CreateDbTask extends Task {
  override type Config = Unit

  override def emptyConfig: Unit = Unit

  override def definition: TaskDefinition[Unit] = new TaskDefinition[Unit](
    name = "create-db",
    description = "Creates the aetr server db"
  )

  override def execute(args: Unit): Unit = {
    Guice.createInjector(Modules(): _*).instance[DbInitializer].init()
  }
}
