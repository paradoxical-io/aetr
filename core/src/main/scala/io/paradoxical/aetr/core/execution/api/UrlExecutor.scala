package io.paradoxical.aetr.core.execution.api

import io.paradoxical.aetr.core.execution.{ExecutionResult, RunToken}
import io.paradoxical.aetr.core.model.ResultData
import java.net.URL
import scala.util.Try

trait UrlExecutor {
  // POST url?aetr=runToken <data>
  def execute(token: RunToken, url: URL, data: Option[ResultData]): Try[ExecutionResult]
}


