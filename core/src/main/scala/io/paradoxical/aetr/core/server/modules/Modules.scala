package io.paradoxical.aetr.core.server.modules

import com.google.inject.{Module, Provides}
import com.twitter.inject.TwitterModule
import io.paradoxical.aetr.core.config.{ConfigLoader, ServiceConfig}
import io.paradoxical.aetr.core.db.PostgresDbProvider
import io.paradoxical.finatra.modules.Defaults
import io.paradoxical.jackson.JacksonSerializer
import io.paradoxical.rdb.slick.providers.{DataSourceProviders, SlickDBProvider}
import java.time.Clock
import javax.inject.Singleton
import javax.sql.DataSource
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcProfile

object Modules {
  def apply(
    config: Option[ServiceConfig] = None
  )(implicit executionContext: ExecutionContext): List[Module] = {
    List(
      new ConfigModule(config),
      new PostgresModule,
      new ClockModule(),
      new JacksonModule()
    ) ++
    Defaults()
  }
}

class ClockModule(clock: Clock = Clock.systemUTC()) extends TwitterModule {
  protected override def configure(): Unit = {
    bind[Clock].toInstance(clock)
  }
}

class ConfigModule(config: Option[ServiceConfig] = None) extends TwitterModule {
  @Provides
  @Singleton
  def c: ServiceConfig = {
    config.getOrElse(ConfigLoader.load())
  }
}

class JacksonModule extends TwitterModule {
  protected override def configure(): Unit = {
    bind[JacksonSerializer].toInstance(new JacksonSerializer())
  }
}

class PostgresModule extends TwitterModule {
  protected override def configure(): Unit = {
    bind[SlickDBProvider].to[PostgresDbProvider].asEagerSingleton()
  }

  @Provides
  def driver(slickDBProvider: SlickDBProvider): JdbcProfile = slickDBProvider.driver

  @Provides
  @Singleton
  def dataSource(config: ServiceConfig): DataSource = {
    DataSourceProviders.default(config.db)
  }
}