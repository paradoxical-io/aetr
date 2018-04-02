package io.paradoxical.aetr.core.execution.api

import io.paradoxical.aetr.core.execution.RunToken
import io.paradoxical.aetr.core.model.ResultData
import java.net.URL

trait UrlExecutor {
  // POST url?aetr=runToken <data>
  def execute(token: RunToken, url: URL, data: Option[ResultData]): Unit
}
