package io.paradoxical.aetr.core.server.controllers

import com.twitter.finagle.http.Request
import io.paradoxical.finatra.Framework

class PingController extends Framework.RestApi {
  getWithDoc("/ping") {
    _.description("Ping API")
  } { _: Request =>
    "pong"
  }
}
