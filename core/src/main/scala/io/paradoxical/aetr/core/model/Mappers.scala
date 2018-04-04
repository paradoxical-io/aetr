package io.paradoxical.aetr.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import javax.script.{ScriptContext, ScriptEngineManager, SimpleScriptContext}
import jdk.nashorn.api.scripting.JSObject

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  defaultImpl = classOf[NoOp],
  property = "type")
@JsonSubTypes(value = Array(
  new Type(value = classOf[Mappers.Identity], name = "no-op"),
  new Type(value = classOf[Mappers.Nashorn], name = "js")
))
sealed trait Mapper {
  def map(in: ResultData): ResultData
}

object Mappers {
  case class Identity() extends Mapper {
    override def map(in: ResultData): ResultData = in
  }

  // used for testing only
  case class Function(f: ResultData => ResultData) extends Mapper {
    override def map(in: ResultData): ResultData = f(in)
  }

  object Nashorn {
    protected val engine = new ScriptEngineManager().getEngineByName("nashorn")
  }

  case class Nashorn(js: String) extends Mapper {

    import Nashorn._

    override def map(in: ResultData): ResultData = {
      val context = new SimpleScriptContext

      context.setBindings(engine.createBindings, ScriptContext.ENGINE_SCOPE)

      engine.eval(js, context)

      val function = context.getAttribute("apply", ScriptContext.ENGINE_SCOPE).asInstanceOf[JSObject]

      val result = function.call(null, in.value).asInstanceOf[String]

      ResultData(result)
    }
  }
}
