package io.paradoxical.aetr

import com.google.inject.Guice
import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import io.paradoxical.aetr.core.db.DbInitializer
import io.paradoxical.aetr.core.server.AetrServer
import io.paradoxical.aetr.core.server.modules.Modules
import io.paradoxical.aetr.db.PostgresDbTestBase
import net.codingwell.scalaguice.InjectorExtensions._
import scala.concurrent.ExecutionContext.Implicits.global

class ServiceTests extends PostgresDbTestBase {
  val modules = Modules(config = Some(newDbAndConfig))

  lazy val server = new EmbeddedHttpServer(new AetrServer(modules))

  "Server" should "ping" in {
    server.httpGet(
      path = "/ping",
      andExpect = Ok,
      withBody = "pong")
  }

  override protected def beforeAll(): Unit = {
    Guice.createInjector(modules: _*).instance[DbInitializer].init()
  }

  override protected def afterAll(): Unit = {
    server.close()

    super.afterAll()
  }
}
