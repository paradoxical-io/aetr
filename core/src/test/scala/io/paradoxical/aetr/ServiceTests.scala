package io.paradoxical.aetr

import com.twitter.finagle.http.Status.Ok
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.server.AetrServer
import io.paradoxical.aetr.core.server.modules.{Modules, PostgresModule}
import io.paradoxical.aetr.db.MockStorageModule
import io.paradoxical.aetr.db.TestModules._
import org.scalatest.mockito.MockitoSugar
import scala.concurrent.ExecutionContext.Implicits.global

class ServiceTests extends FeatureTest with MockitoSugar {
  val modules = Modules().ignore[PostgresModule].overlay(new MockStorageModule(mock[StepDb]))

  override val server = new EmbeddedHttpServer(new AetrServer(modules))

  test("server#ping") {
    server.httpGet(
      path = "/ping",
      andExpect = Ok,
      withBody = "pong")
  }
}
