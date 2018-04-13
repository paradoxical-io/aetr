package io.paradoxical.aetr.core.server

import com.google.inject.Module
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.{BroadcastStatsReceiver, LoadedStatsReceiver, StatsReceiver}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.TwitterModule
import com.twitter.util.{Await, Duration}
import io.paradoxical.aetr.core.lifecycle.Startup
import io.paradoxical.aetr.core.server.controllers._
import io.paradoxical.aetr.core.server.serialization.JsonModule
import io.paradoxical.aetr.core.stats.FinagleStatsBridgeReceiver
import io.paradoxical.ecsv.tasks.server.controllers.UiController
import io.paradoxical.finatra.swagger.{ApiDocumentationConfig, SwaggerDocs}

class AetrServer(override val modules: Seq[Module]) extends HttpServer with SwaggerDocs {
  // register the shutdown hook
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {
      Await.result(close(Duration.fromSeconds(30)))

      logger.info("Goodbye!")
    }
  })

  // Manually add the bridge receiver so we don't have to rely on service loading
  override protected def statsReceiverModule: Module = new TwitterModule {
    protected override def configure(): Unit = {
      bindSingleton[StatsReceiver].toInstance(
        BroadcastStatsReceiver(Seq(new FinagleStatsBridgeReceiver, LoadedStatsReceiver))
      )
    }
  }

  override def defaultFinatraHttpPort = ":9999"

  override def documentation = new ApiDocumentationConfig {
    override val description: String = "Aetr"
    override val title: String = "API"
    override val version: String = "1.0"
  }

  override protected def jacksonModule: Module = new JsonModule()

  protected override def postWarmup(): Unit = {
    val startup = injector.instance[Startup]

    startup.start()

    onExit {
      startup.stop()
    }

    super.postWarmup()
  }

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]

    // create swagger
    swaggerInfo

    configureDocumentation(router)

    router
      .add[PingController]
      .add[StepsController]
      .add[RunsController]
      .add[DebugController]
      .add[UiController]
  }
}
