package io.paradoxical.aetr.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import javax.script.{ScriptContext, SimpleScriptContext}
import jdk.nashorn.api.scripting.{ClassFilter, JSObject}

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type")
@JsonSubTypes(value = Array(
  new Type(value = classOf[Reducers.NoOp], name = "no-op"),
  new Type(value = classOf[Reducers.Last], name = "last"),
  new Type(value = classOf[Reducers.Nashorn], name = "js")
))
sealed trait Reducer {
  def reduce(ins: Seq[ResultData]): Option[ResultData]
}

object Reducers {
  case class NoOp() extends Reducer {
    override def reduce(ins: Seq[ResultData]): Option[ResultData] = None
  }

  case class Last() extends Reducer {
    override def reduce(ins: Seq[ResultData]): Option[ResultData] = ins.lastOption
  }

  case class Function(f: Seq[ResultData] => Option[ResultData]) extends Reducer {
    override def reduce(ins: Seq[ResultData]): Option[ResultData] = f(ins)
  }



  class NoJavaFilter extends ClassFilter {
    def exposeToScripts(s: String) = false
  }

  case class Nashorn(js: String) extends Reducer {

    import io.paradoxical.aetr.core.model.nashorn.NashornEngine._
    import jdk.nashorn.api.scripting.ScriptObjectMirror

    override def reduce(ins: Seq[ResultData]): Option[ResultData] = {
      val context = new SimpleScriptContext

      context.setBindings(engine.createBindings, ScriptContext.ENGINE_SCOPE)

      engine.eval(js, context)

      val function = context.getAttribute("apply", ScriptContext.ENGINE_SCOPE).asInstanceOf[JSObject]

      val nativeJsInitializer = s"var arr = [${ins.map(x => s""""${x.value}"""").mkString(",")}]; arr"

      val jsArray = engine.eval(nativeJsInitializer).asInstanceOf[ScriptObjectMirror]

      val result = function.call(null, jsArray).asInstanceOf[String]

      Option(result).map(ResultData)
    }
  }
}
