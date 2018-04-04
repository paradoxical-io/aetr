package io.paradoxical.aetr.core.execution.api

import io.paradoxical.aetr.core.execution.RunToken
import io.paradoxical.aetr.core.model.ResultData
import java.net.URL
import scalaj.http.Http

class UrlExecutorImpl extends UrlExecutor {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  override def execute(token: RunToken, url: URL, data: Option[ResultData]): Unit = {
    logger.info(s"Executing run token $token")
    val port = if (url.getPort == -1) "" else s":${url.getPort}"
    val queryMap = Option(url.getQuery).getOrElse("").split("&").toList.foldLeft(Map.empty[String, List[String]]) {
      case (accum, q) =>
        q.split("=").toList match {
          case key :: value :: Nil => accum.updated(key, accum.getOrElse(key, Nil) :+ value)
          case x => throw new IllegalArgumentException(s"Illegal format of query string (str = $x). Full URL = $url")
        }
    }

    val queryString = "?" + (queryMap + (("aetr", token.asRaw :: Nil))).map { case (k, v) => s"$k=${v.mkString(",")}" }.reduceLeft(_ + "&" + _)

    val sanitizedUrl = s"${url.getProtocol}://${url.getHost}$port${url.getPath}$queryString"

    val req = if (data.isDefined) {
      Http(sanitizedUrl).postData(data.get.value)
    } else {
      Http(sanitizedUrl)
    }

    // Expose the response?
    // Throw on >= 400?
    req.execute()
  }
}
