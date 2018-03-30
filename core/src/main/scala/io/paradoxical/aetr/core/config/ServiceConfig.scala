package io.paradoxical.aetr.core.config

import io.paradoxical.rdb.hikari.config.RdbConfigWithConnectionPool

case class ServiceConfig(
  maxAtomicRetries: Int = 10,
  db: RdbConfigWithConnectionPool
)
