package io.paradoxical.aetr.core

import io.paradoxical.aetr.core.server.ServerTask
import io.paradoxical.tasks.{Task, TaskEnabledApp}

object Main extends TaskEnabledApp {
  override def appName: String = "aetr"

  lazy val serverTasks = new ServerTask

  override def tasks: List[Task] = List(serverTasks)
}