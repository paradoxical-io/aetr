import sbt.Keys.{javaOptions, resolvers, _}
import sbt._
import sbtassembly.AssemblyKeys._

object BuildConfig {
  lazy val versions = new {
    val finatra = "2.13.0"
    val guice = "4.0"
    val logback = "1.1.7"
    val mockito = "1.9.5"
    val scalatest = "3.0.0"
    val scalacheck = "1.13.4"
  }

  object Dependencies {
    val testDeps = Seq(
      "org.mockito" % "mockito-core" % versions.mockito % "test",
      "org.scalacheck" %% "scalacheck" % versions.scalacheck % "test",
      "org.scalatest" %% "scalatest" % versions.scalatest % "test"
    )
  }
  object Revision {
    lazy val revision = System.getProperty("revision", "SNAPSHOT")
  }

  def commonSettings(currentVersion: String) = {
    Seq(
      fork in run := true,

      test in assembly := {},

      javaOptions ++= Seq(
        "-Dlog.service.output=/dev/stderr",
        "-Dlog.access.output=/dev/stderr"),

      resolvers += Resolver.sonatypeRepo("releases"),

      organization := "io.paradoxical",

      version := s"${currentVersion}-${BuildConfig.Revision.revision}",

      scalaVersion := "2.12.4",

      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "UTF-8",
        "-feature",
        "-language:existentials",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-language:postfixOps",
        "-language:experimental.macros",
        "-unchecked",
        "-Ywarn-nullary-unit",
        "-Xfatal-warnings",
        "-Xlint",
        "-Ywarn-dead-code",
        "-Xfuture"
      ),

      scalacOptions in doc ++= scalacOptions.value.filterNot(_ == "-Xfatal-warnings"),

      sources in doc in Compile := List()
    )
  }
}
