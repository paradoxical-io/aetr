package io.paradoxical.aetr.core.server

import io.paradoxical.aetr.core.server.modules.Modules
import io.paradoxical.tasks.{Task, TaskDefinition}
import scala.concurrent.ExecutionContext.Implicits.global

class ServerTask extends Task {
  override type Config = Unit

  override def emptyConfig: Unit = Unit

  override def definition: TaskDefinition[Unit] = new TaskDefinition[Unit](
    name = "server",
    description = "Runs the aetr server"
  )

  override def execute(args: Unit): Unit = {
    new AetrServer(Modules()).main(Array.empty)
  }
}