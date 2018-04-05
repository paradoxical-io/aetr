package io.paradoxical.ecsv.tasks.server.controllers

import com.twitter.finagle.http.Request
import io.paradoxical.finatra.Framework
import javax.inject.Inject

class UiController @Inject()() extends Framework.AssetsApi {
  private val basePath = "/ui/dist"

  get("/:*") { request: Request =>
    staticFileOrIndex(request, s"${basePath}/${request.params("*")}", s"${basePath}/index.html")
  }
}
