package io.paradoxical.aetr.db

import com.google.inject.{Guice, Injector}
import io.paradoxical.aetr.TestBase
import io.paradoxical.aetr.core.config.ConfigLoader
import io.paradoxical.aetr.core.server.modules.Modules
import io.paradoxical.rdb.config.RdbCredentials
import net.codingwell.scalaguice.InjectorExtensions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class PostgresDbTestBase extends TestBase {
  val docker = Postgres.docker()

  def withDb(test: Injector => Any): Unit = {
    val db = "test_db_" + Math.abs(Random.nextInt())

    val defaultConfig = ConfigLoader.load()

    docker.createDatabase(db)

    val testConfig = defaultConfig.copy(db = defaultConfig.db.copy(
      url = docker.url(db),
      credentials = RdbCredentials(
        user = docker.user,
        password = docker.password
      )
    ))

    val injector = Guice.createInjector(Modules(config = Some(testConfig)): _*)

    injector.instance[DbInitializer].init()

    test(injector)
  }

  override protected def afterAll(): Unit = {
    docker.close()
  }
}
