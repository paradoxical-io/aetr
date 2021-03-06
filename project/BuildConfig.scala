import sbt.Keys.{javaOptions, resolvers, _}
import sbt._
import sbtassembly.AssemblyKeys._

object BuildConfig {
  lazy val versions = new {
    lazy val paradox = new {
      val finatra = "1.0.5"
      val global = "1.2"
      val tasks = "1.5"
      val slick = "1.0"
      val docker = "1.24"
    }
    val ficus = "1.4.3"
    val typesafeConfig = "1.3.3"
    val julSlf4j = "1.7.25"
    val janino = "2.7.8"
    val dropwizardMetrics = "4.0.0"
    val guice = "4.0"
    val mockito = "1.9.5"
    val logback = "1.1.7"
    val scalatest = "3.0.0"
    val scalacheck = "1.13.4"
    val scalaGuice = "4.1.1"
    val postgres = "42.2.2"
    val scalajhttp = "2.3.0"
    val okhttp = "3.10.0"
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
       // "-Ywarn-dead-code",
        "-Xfuture"
      ),

      fork in Test := true,

      scalacOptions in(Compile, doc) := scalacOptions.value.filterNot(_ == "-Xfatal-warnings"),
      scalacOptions in(Compile, doc) += "-no-java-comments"
    ) ++ Publishing.publishSettings
  }
}
