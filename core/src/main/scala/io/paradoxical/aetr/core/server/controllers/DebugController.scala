package io.paradoxical.aetr.core.server.controllers

import com.twitter.finagle.http.Status
import com.twitter.finatra.request.QueryParam
import io.paradoxical.finatra.Framework
import io.paradoxical.finatra.execution.TwitterExecutionContextProvider
import java.util.concurrent.{Executors, TimeUnit}
import javax.inject.Inject
import scalaj.http.Http

class DebugController @Inject()() extends Framework.RestApi {
  private val executor = TwitterExecutionContextProvider.of(Executors.newSingleThreadScheduledExecutor())

  post("/debug/api/v1/execute") { req: DebugExecuteRequest =>
    logger.info(s"Scheduling request to ${req.aetr} in ${req.waitTimeSeconds} seconds")

    executor.schedule(new Runnable {
      override def run(): Unit = {
        logger.info(s"Processing ${req.aetr}")

       val result = Http(req.aetr).postData(System.currentTimeMillis().toString).execute[String]()

        if(result.is2xx) {
          logger.info("Successfully completed")
        } else {
          logger.warn("Unable to complete")
        }
      }
    }, req.waitTimeSeconds, TimeUnit.SECONDS)

    Status.Ok
  }
}

case class DebugExecuteRequest(@QueryParam aetr: String, @QueryParam waitTimeSeconds: Int = 10)
