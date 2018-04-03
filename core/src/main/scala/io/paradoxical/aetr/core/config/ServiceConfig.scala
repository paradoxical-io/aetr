package io.paradoxical.aetr.core.config

import io.paradoxical.rdb.hikari.config.RdbConfigWithConnectionPool
import scala.concurrent.duration.{FiniteDuration, _}

case class ServiceConfig(
  maxAtomicRetries: Int = 10,
  db: RdbConfigWithConnectionPool,
  dbLockTime: FiniteDuration = 60 seconds,
  threadSleepOnDequeueError: FiniteDuration = 10 seconds,
  pendingPollTime: FiniteDuration = 30 seconds,
  stats: StatsConfig
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
