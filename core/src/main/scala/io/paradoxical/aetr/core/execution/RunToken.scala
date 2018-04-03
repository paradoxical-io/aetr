package io.paradoxical.aetr.core.execution

import io.paradoxical.aetr.core.model.{RootId, RunInstanceId}
import java.util.UUID

object RunToken {
  def apply(string: String): RunToken = {
    string.split(":").map(UUID.fromString).toList match {
      case id :: root :: _ => RunToken(RunInstanceId(id), RootId(root))
      case _ => throw new RuntimeException(s"Token not in correct format $string")
    }
  }
}

case class RunToken(runId: RunInstanceId, rootId: RootId) {
  def asRaw = s"$runId:$rootId"
}