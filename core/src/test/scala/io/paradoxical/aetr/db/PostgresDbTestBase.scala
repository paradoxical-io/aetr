package io.paradoxical.aetr.db

import com.google.inject.{Guice, Injector, Module}
import com.twitter.inject.TwitterModule
import com.zaxxer.hikari.HikariDataSource
import io.paradoxical.aetr.TestBase
import io.paradoxical.aetr.core.config.{ConfigLoader, ServiceConfig}
import io.paradoxical.aetr.core.db.DbInitializer
import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.server.modules.Modules
import io.paradoxical.rdb.config.RdbCredentials
import javax.sql.DataSource
import net.codingwell.scalaguice.InjectorExtensions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Random, Try}

class PostgresDbTestBase extends TestBase {
  lazy val docker = Postgres.docker()

  def withDb(test: Injector => Any): Unit = {
    val testConfig = newDbAndConfig

    val injector = Guice.createInjector(Modules(config = Some(testConfig)): _*)

    injector.instance[DbInitializer].init()

    test(injector)

    Try(injector.instance[DataSource].asInstanceOf[HikariDataSource].close())
  }

  protected def newDbAndConfig: ServiceConfig = {
    val db = "test_db_" + Math.abs(Random.nextInt())

    val defaultConfig = ConfigLoader.load()

    docker.createDatabase(db)

    defaultConfig.copy(db = defaultConfig.db.copy(
      url = docker.url(db),
      credentials = RdbCredentials(
        user = docker.user,
        password = docker.password
      )
    ))
  }

  override protected def afterAll(): Unit = {
    docker.close()
  }
}


object TestModules {
  def apply(config: ServiceConfig) = Modules(config = Some(config))

  implicit class WithModules(modules: List[Module]) {
    def overlay(overrideModules: Module*): List[Module] = {
      List(com.google.inject.util.Modules.`override`(modules: _*).`with`(overrideModules: _*))
    }

    def ignore[T <: Module : Manifest] = {
      modules.filterNot(_.getClass == manifest[T].runtimeClass)
    }
  }
}

class MockStorageModule(stepDb: StepDb) extends TwitterModule {
  protected override def configure(): Unit = {
    bind[StepDb].toInstance(stepDb)
  }
}