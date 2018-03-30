package io.paradoxical.aetr

import io.paradoxical.aetr.db.Postgres

class DbTests extends TestBase {
  val docker = Postgres.docker()

  override protected def afterAll(): Unit = {
    docker.close()
  }
}
