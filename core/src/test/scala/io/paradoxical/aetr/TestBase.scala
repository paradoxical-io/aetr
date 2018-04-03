package io.paradoxical.aetr

import com.twitter.util
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class TestBase extends FlatSpec with Matchers with BeforeAndAfterAll with MockitoSugar {
  util.logging.Slf4jBridgeUtility.attemptSlf4jBridgeHandlerInstallation()
}
