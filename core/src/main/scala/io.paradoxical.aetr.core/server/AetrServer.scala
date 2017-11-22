package io.paradoxical.aetr.core.server

import com.google.inject.Module
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import io.paradoxical.aetr.core.server.controllers.PingController
import io.paradoxical.aetr.core.server.serialization.JsonModule
import io.paradoxical.finatra.swagger.{ApiDocumentationConfig, SwaggerDocs}

class AetrServer extends HttpServer with SwaggerDocs {
  override def defaultFinatraHttpPort = ":9999"

  override def documentation = new ApiDocumentationConfig {
    override val description: String = "Aetr"
    override val title: String = "API"
    override val version: String = "1.0"
  }

  override protected def jacksonModule: Module = new JsonModule()

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[PingController]

    configureDocumentation(router)
  }
}
