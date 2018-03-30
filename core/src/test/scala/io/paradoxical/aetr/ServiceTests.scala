package io.paradoxical.aetr

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import io.paradoxical.aetr.core.server.AetrServer
import io.paradoxical.aetr.core.server.modules.Modules
import scala.concurrent.ExecutionContext.Implicits.global

class ServiceTests extends FeatureTest {

  override val server = new EmbeddedHttpServer(new AetrServer(Modules()))

  test("server#ping") {
    server.httpGet(
      path = "/ping",
      andExpect = Ok,
      withBody = "pong")
  }
}
