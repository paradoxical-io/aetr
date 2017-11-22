package com.example

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.Test
import io.paradoxical.aetr.core.server.AetrServer

class ExampleStartupTest extends Test {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new AetrServer)

  test("server#startup") {
    server.assertHealthy()
  }
}
