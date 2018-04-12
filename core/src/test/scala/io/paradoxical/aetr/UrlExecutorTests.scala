package io.paradoxical.aetr

import io.paradoxical.aetr.core.config.ConfigLoader
import io.paradoxical.aetr.core.db.StepsDbSync
import io.paradoxical.aetr.core.execution.{ArbitraryStringExecutionResult, ExecutionHandler}
import io.paradoxical.aetr.core.execution.api.UrlExecutorImpl
import io.paradoxical.aetr.core.graph.RunManager
import io.paradoxical.aetr.core.model._
import java.net.URL
import okhttp3.HttpUrl
import okhttp3.mockwebserver.{MockResponse, MockWebServer}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertions, FlatSpec, Inside}
import scala.util.Success

class UrlExecutorTests extends FlatSpec with Assertions with MockitoSugar with Inside {
  private val config = ConfigLoader.load()

  "UrlExecutor" should "make requests with a run token" in withTestServer {
    case TestServerContext(server, url) =>
      val expectedResponseBody = "good show"
      server.enqueue(new MockResponse().setBody(expectedResponseBody))

      val mockdb = mock[StepsDbSync]

      val callbackUrl = url.newBuilder().addPathSegment("execute").build().toString
      val tree = Action(name = NodeName("test"), execution = ApiExecution(url = new URL(callbackUrl)))

      val run = new RunManager(tree).root

      val exec = new UrlExecutorImpl(config)

      val result = new ExecutionHandler(mockdb, exec).execute(Actionable(run, tree, None))

      assert(result.isSuccess)

      val recordedRequest = server.takeRequest()

      val expectedPath = s"/execute?aetr=${config.meta.host}/api/v1/runs/complete?token=${run.token.asRaw}"
      assert(recordedRequest.getPath === expectedPath)
      inside(result) {
        case Success(ArbitraryStringExecutionResult(responseBody)) =>
          assert(responseBody === expectedResponseBody)
      }
  }

  it should "sends request bodies" in withTestServer {
    case TestServerContext(server, url) =>
      val expectedResponseBody = "good show!!"
      val expectedResponseBody2 = "bad show!!"
      server.enqueue(new MockResponse().setBody(expectedResponseBody))
      server.enqueue(new MockResponse().setBody(expectedResponseBody2))

      val mockdb = mock[StepsDbSync]

      val callbackUrl = url.newBuilder().
        addPathSegment("execute").
        addQueryParameter("q", "1").
        addQueryParameter("q", "2").
        addQueryParameter("q2", "hello").
        build().toString

      val tree = new DependentTree().makeTree(List.fill(2)(callbackUrl): _*)

      val run = new RunManager(tree)

      val exec = new UrlExecutorImpl(config)

      val action1 = run.next().head

      val result1 = new ExecutionHandler(mockdb, exec).execute(Actionable(action1.run, action1.action, action1.previousResult))

      result1 match {
        case Success(ArbitraryStringExecutionResult(res)) =>
          assert(res === expectedResponseBody)
          run.complete(action1.run, result = Some(ResultData(res)))
        case _ => fail()
      }

      val action2 = run.next().head

      val result2 = new ExecutionHandler(mockdb, exec).execute(Actionable(action2.run, action2.action, action2.previousResult))

      inside(result2) {
        case Success(ArbitraryStringExecutionResult(res)) =>
          assert(res === expectedResponseBody2)
      }

      val recordedRequest1 = server.takeRequest()

      val expectedAetrCallbackFmt = s"${config.meta.host}/api/v1/runs/complete?token="
      assert(recordedRequest1.getPath === s"/execute?q=1,2&q2=hello&aetr=$expectedAetrCallbackFmt${action1.run.token.asRaw}")
      assert(recordedRequest1.getBody.size() === 0L)

      val recordedRequest2 = server.takeRequest()

      assert(recordedRequest2.getPath === s"/execute?q=1,2&q2=hello&aetr=$expectedAetrCallbackFmt${action2.run.token.asRaw}")
      assert(action2.previousResult.get.value === new String(recordedRequest2.getBody.snapshot().toByteArray))
  }

  def withTestServer(test: TestServerContext => Any) = {
    val server = new MockWebServer
    val url = server.url("/")
    val ctx = TestServerContext(server, url)
    try {
      test(ctx)
    } finally {
      server.close()
    }
  }

  class DependentTree {
    val rootId = StepTreeId.next

    val action1: Action = Action(
      id = StepTreeId.next,
      name = NodeName("action1"),
      mapper = Mappers.Function(res => ResultData(res.value + "_mapped")),
    )

    val action2 = Action(
      id = StepTreeId.next,
      name = NodeName("action2")
    )

    val actionSeq = Seq(action1, action2)

    val sequentialParent = SequentialParent(
      name = NodeName("SequentialParent"),
      id = StepTreeId.next
    )

    val treeRoot = SequentialParent(name = NodeName("root"), id = rootId)

    def makeTree(urls: String*) = {
      val actionsWithUrls = actionSeq.zip(urls).map {
        case (action, url) => action.copy(execution = ApiExecution(url = new URL(url)))
      }

      val seqParent = actionsWithUrls.foldLeft(sequentialParent: Parent)(_.addTree(_))

      treeRoot.addTree(seqParent)
    }
  }

  case class TestServerContext(server: MockWebServer, serverUrl: HttpUrl)
}
