package com.example

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import io.paradoxical.aetr.core.server.ExampleServer

class ExampleFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new ExampleServer)

  test("server#ping") {
    server.httpGet(
      path = "/ping",
      andExpect = Ok,
      withBody = "pong")
  }
}
