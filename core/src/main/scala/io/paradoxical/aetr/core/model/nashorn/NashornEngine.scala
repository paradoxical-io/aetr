package io.paradoxical.aetr.core.model.nashorn

import io.paradoxical.aetr.core.model.Reducers.NoJavaFilter

object NashornEngine {
  val engine = {
    import jdk.nashorn.api.scripting.NashornScriptEngineFactory

    val factory = new NashornScriptEngineFactory

    // don't allow access to the underlying java types
    factory.getScriptEngine(new NoJavaFilter())
  }
}
