import sbt.internal.inc.Analysis
import scala.sys.process._

/**
 * UI compilation phases
 */
lazy val UI = ConfigKey("ui")

lazy val dev = taskKey[Unit]("Run the UI in dev mode")

(compile in UI) := {
  val r = ((baseDirectory in ThisBuild).value / "webapp-build.sh").getAbsolutePath !

  if (r != 0) {
    throw new RuntimeException("Ui was unable to compile")
  }

  Analysis.Empty
}

(test in UI) := {
  ((baseDirectory in ThisBuild).value / "webapp-test.sh").getAbsolutePath !
}

(dev in UI) := {
  streams.value.log.info("Open http://localhost:4200")

  ((baseDirectory in ThisBuild).value / "webapp-hotswap-ui.sh").getAbsolutePath !
}