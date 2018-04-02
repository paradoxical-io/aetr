package io.paradoxical.aetr.core.config

import io.paradoxical.rdb.hikari.config.RdbConfigWithConnectionPool
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

case class ServiceConfig(
  maxAtomicRetries: Int = 10,
  db: RdbConfigWithConnectionPool,
  dbLockTime: FiniteDuration = 60 seconds
)
