//package com.example
//
//import com.twitter.finagle.http.Status.Ok
//import com.twitter.finatra.http.EmbeddedHttpServer
//import com.twitter.inject.server.FeatureTest
//import io.paradoxical.aetr.core.server.AetrServer
//import io.paradoxical.aetr.core.server.modules.Modules
//
//class ExampleFeatureTest extends FeatureTest {
//
//  override val server = new EmbeddedHttpServer(new AetrServer(Modules()))
//
//  test("server#ping") {
//    server.httpGet(
//      path = "/ping",
//      andExpect = Ok,
//      withBody = "pong")
//  }
//}
