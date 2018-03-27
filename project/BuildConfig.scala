import sbt.Keys.{javaOptions, resolvers, _}
import sbt._
import sbtassembly.AssemblyKeys._

object BuildConfig {
  lazy val versions = new {
    lazy val paradox = new {
      val finatra = "1.0.4"
      val global = "1.1"
      val tasks = "1.5"
    }
    val guice = "4.0"
    val mockito = "1.9.5"
    val logback = "1.1.7"
    val scalatest = "3.0.0"
    val scalacheck = "1.13.4"
    val scalaGuice = "4.1.1"
  }

  object Dependencies {
    val testDeps = Seq(
      "org.mockito" % "mockito-core" % versions.mockito % "test",
      "org.scalacheck" %% "scalacheck" % versions.scalacheck % "test",
      "org.scalatest" %% "scalatest" % versions.scalatest % "test"
    )
  }
  object Revision {
    lazy val version = System.getProperty("version", "1.0-SNAPSHOT")
  }

  def commonSettings() = {
    Seq(
      fork in run := true,

      test in assembly := {},

      javaOptions ++= Seq(
        "-Dlog.service.output=/dev/stderr",
        "-Dlog.access.output=/dev/stderr"),

      resolvers += Resolver.sonatypeRepo("public"),

      organization := "io.paradoxical",

      version := BuildConfig.Revision.version,

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

      scalacOptions in(Compile, doc) := scalacOptions.value.filterNot(_ == "-Xfatal-warnings"),
      scalacOptions in(Compile, doc) += "-no-java-comments"
    ) ++ Publishing.publishSettings
  }
}
