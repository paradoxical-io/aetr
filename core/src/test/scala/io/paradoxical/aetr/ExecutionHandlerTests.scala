package io.paradoxical.aetr

import io.paradoxical.aetr.core.db.StepsDbSync
import io.paradoxical.aetr.core.execution.api.UrlExecutor
import io.paradoxical.aetr.core.execution.{EmptyExecutionResult, ExecutionHandler}
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model._
import java.net.URL
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

class ExecutionHandlerTests extends TestBase {
  "Execution handler" should "mark actions as errors" in {
    val db = mock[StepsDbSync]

    val urlExecutor = mock[UrlExecutor]

    val tree = Action(name = NodeName("test"), execution = ApiExecution(url = new URL("http://foo")))

    val run = new RunManager(tree).root

    val stateCapture = ArgumentCaptor.forClass(classOf[RunState])
    val resultsCapture = ArgumentCaptor.forClass(classOf[Option[ResultData]])

    val errorMessage = "Url execution failed"

    when(urlExecutor.execute(any(), any(), any())).thenReturn(Failure(new RuntimeException(errorMessage)))

    new ExecutionHandler(db, urlExecutor).execute(
      Actionable(run, tree, None)
    )

    verify(db, times(1)).trySetRunState(any(), any(), stateCapture.capture(), resultsCapture.capture())

    stateCapture.getAllValues.asScala.last shouldEqual RunState.Error

    resultsCapture.getAllValues.asScala.last shouldEqual Some(ResultData(errorMessage))
  }

  it should "mark actions as executing on success" in {
    val db = mock[StepsDbSync]

    val urlExecutor = mock[UrlExecutor]

    when(urlExecutor.execute(any(), any(), any())).thenReturn(Success(EmptyExecutionResult))

    val tree = Action(name = NodeName("test"), execution = ApiExecution(url = new URL("http://foo")))

    val run = new RunManager(tree).root

    val stateResults = ArgumentCaptor.forClass(classOf[RunState])

    new ExecutionHandler(db, urlExecutor).execute(
      Actionable(run, tree, None)
    )

    verify(db, times(1)).trySetRunState(any(), any(), stateResults.capture(), any())

    stateResults.getAllValues.asScala.last shouldEqual RunState.Executing
  }
}
