package io.paradoxical.aetr.core.server.modules

import com.google.inject.Module
import com.twitter.inject.TwitterModule
import io.paradoxical.finatra.modules.Defaults
import scala.concurrent.ExecutionContext

object Modules {
  def apply()(implicit executionContext: ExecutionContext): List[Module] = Defaults()
}

class PostgresModule extends TwitterModule {
  ???
}