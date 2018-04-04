package io.paradoxical.aetr.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import java.net.URL

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  defaultImpl = classOf[NoOp],
  property = "type")
@JsonSubTypes(value = Array(
  new Type(value = classOf[ApiExecution], name = "api"),
  new Type(value = classOf[NoOp], name = "no-op")
))
sealed trait Execution
case class ApiExecution(url: URL) extends Execution
case class NoOp() extends Execution