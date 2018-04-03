package io.paradoxical.aetr.core.execution.api

import io.paradoxical.aetr.core.execution.RunToken
import io.paradoxical.aetr.core.model.ResultData
import java.net.URL

class UrlExecutorImpl extends UrlExecutor {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  override def execute(token: RunToken, url: URL, data: Option[ResultData]): Unit = {
    logger.info(s"Executing run token $token")
  }
}
