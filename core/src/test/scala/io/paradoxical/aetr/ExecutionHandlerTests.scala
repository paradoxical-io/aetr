package io.paradoxical.aetr

import io.paradoxical.aetr.core.db.StepsDbSync
import io.paradoxical.aetr.core.execution.ExecutionHandler
import io.paradoxical.aetr.core.execution.api.UrlExecutor
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model._
import java.net.URL
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import scala.collection.JavaConverters._

class ExecutionHandlerTests extends TestBase {
  "Execution handler" should "mark actions as errors" in {
    val db = mock[StepsDbSync]

    val urlExecutor = mock[UrlExecutor]

    val tree = Action(name = NodeName("test"), execution = ApiExecution(url = new URL("http://foo")))

    val run = new RunManager(tree).root

    val stateResults = ArgumentCaptor.forClass(classOf[RunState])

    when(urlExecutor.execute(any(), any(), any())).thenThrow(new RuntimeException("Url execution failed"))

    new ExecutionHandler(db, urlExecutor).execute(
      Actionable(run, tree, None)
    )

    verify(db, times(1)).trySetRunState(any(), stateResults.capture(), any())

    stateResults.getAllValues.asScala.last shouldEqual RunState.Error
  }

  it should "mark actions as executing on success" in {
    val db = mock[StepsDbSync]

    val urlExecutor = mock[UrlExecutor]

    val tree = Action(name = NodeName("test"), execution = ApiExecution(url = new URL("http://foo")))

    val run = new RunManager(tree).root

    val stateResults = ArgumentCaptor.forClass(classOf[RunState])

    new ExecutionHandler(db, urlExecutor).execute(
      Actionable(run, tree, None)
    )

    verify(db, times(1)).trySetRunState(any(), stateResults.capture(), any())

    stateResults.getAllValues.asScala.last shouldEqual RunState.Executing
  }
}
