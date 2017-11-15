package io.paradoxical.aetr.core.server.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

class PingController extends Controller {

  get("/ping") { request: Request =>
    info("ping")
    "pong"
  }
}
