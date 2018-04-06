package io.paradoxical.aetr.core.execution.api

import io.paradoxical.aetr.core.execution.{ArbitraryStringExecutionResult, ExecutionResult, RunToken}
import io.paradoxical.aetr.core.model.ResultData
import java.net.URL
import scalaj.http.Http
import scala.util.Try

class UrlExecutorImpl extends UrlExecutor {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  override def execute(token: RunToken, url: URL, data: Option[ResultData]): Try[ExecutionResult] = {
    logger.info(s"Executing run token $token")
    Try {
      val port = if (url.getPort == -1) "" else s":${url.getPort}"

      val queryMap = buildQueryMap(url)

      val queryString = buildQueryString(queryMap, token)

      val sanitizedUrl = s"${url.getProtocol}://${url.getHost}$port${url.getPath}$queryString"

      val req = if (data.isDefined) {
        Http(sanitizedUrl).postData(data.get.value)
      } else {
        Http(sanitizedUrl)
      }

      // Expose the response?
      // Throw on >= 400?
      val resp = req.execute[String]()

      logger.trace(s"Got ${resp.code} response for run token = $token} callback url = $url: ${resp.body}")

      // Kinda crappy because we lose all HTTP context, but we can figure that out later.
      ArbitraryStringExecutionResult(resp.body)
    }
  }

  private def buildQueryMap(url: URL): Map[String, List[String]] = {
    Option(url.getQuery) match {
      case Some(query) =>
        query.split("&").toList.foldLeft(Map.empty[String, List[String]]) {
          case (accum, q) =>
            q.split("=").toList match {
              case key :: value :: Nil => accum.updated(key, accum.getOrElse(key, Nil) :+ value)
              case x => throw new IllegalArgumentException(s"Illegal format of query string (str = $x). Full URL = $url")
            }
        }

      case None => Map.empty
    }
  }

  private def buildQueryString(queryMap: Map[String, List[String]], runToken: RunToken): String = {
    "?" + (queryMap + (("aetr", runToken.asRaw :: Nil))).
      map { case (k, v) => s"$k=${v.mkString(",")}" }.
      reduceLeft(_ + "&" + _)
  }
}
