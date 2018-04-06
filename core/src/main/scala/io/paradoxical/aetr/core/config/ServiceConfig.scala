package io.paradoxical.aetr.core.config

import io.paradoxical.rdb.hikari.config.RdbConfigWithConnectionPool
import java.net.URL
import scala.concurrent.duration.{FiniteDuration, _}

case class ServiceConfig(
  meta: MetaConfig,
  maxAtomicRetries: Int = 10,
  db: RdbConfigWithConnectionPool,
  dbLockTime: FiniteDuration = 60 seconds,
  threadSleepOnDequeueError: FiniteDuration = 10 seconds,
  pendingPollTime: FiniteDuration = 30 seconds,
  stats: StatsConfig
)

case class MetaConfig(
  host: URL,
  complete_callback_path: String
)

case class StatsConfig(
  print_to_console: Boolean = false,
  graphite: Option[GraphiteConfig]
)

case class GraphiteConfig(
  host: String,
  port: Int,
  interval: FiniteDuration = 10 seconds
)
