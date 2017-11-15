package io.paradoxical.aetr.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser.Feature
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import io.paradoxical.aetr.jackson.serializers.TypeSerializerModule
import scala.reflect.Manifest

class JacksonSerializer(mapper: ObjectMapper with ScalaObjectMapper = JacksonSerializer.default) {
  def fromJson[A: Manifest](string: String): A = {
    mapper.readValue[A](string)
  }

  def toJson[A](item: A): String = {
    mapper.writeValueAsString(item)
  }
}

object JacksonSerializer {
  lazy val default = mapper()

  private def mapper() = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.configure(Feature.IGNORE_UNDEFINED, true).
      configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true).
      configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
      configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true).
      setSerializationInclusion(JsonInclude.Include.NON_NULL).
      setSerializationInclusion(JsonInclude.Include.NON_ABSENT).
      registerModule(TypeSerializerModule).
      registerModule(DefaultScalaModule)
    m
  }
}