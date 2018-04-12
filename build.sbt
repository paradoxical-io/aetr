import BuildConfig._

lazy val commonSettings = BuildConfig.commonSettings()

/**
 * The project root
 */
lazy val `aetr` = project.
  in(file(".")).
  settings(commonSettings).
  settings(
    publish := {},
    aggregate in update := false
  ).aggregate(core)

lazy val core = project.settings(commonSettings).settings(
  Seq(
    name := "aetr-core",

    dockerfile in docker := {
      // The assembly task generates a fat JAR file
      val artifact: File = assembly.value
      val artifactTargetPath = s"/app/${artifact.name}"

      new Dockerfile {
        from("java")
        add(artifact, artifactTargetPath)
        entryPointRaw(s"exec java -jar $artifactTargetPath")
      }
    },

    // keep the log level quiet
    logLevel in assembly := Level.Error,

    assemblyMergeStrategy in assembly := {
      case PathList(ps @ _*) if ps.contains("node_modules") =>
        MergeStrategy.discard
      // relegate back to the previous
      case x @ _ => (assemblyMergeStrategy in assembly).value(x)
    },
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "io.paradoxical" %% "paradox-scala-jackson" % versions.paradox.global,
      "io.paradoxical" %% "paradox-scala-util" % versions.paradox.global,
      "io.paradoxical" %% "slick" % versions.paradox.slick,
      "io.paradoxical" %% "finatra-server" % versions.paradox.finatra,
      "io.paradoxical" %% "finatra-swagger" % versions.paradox.finatra,
      "io.paradoxical" %% "tasks" % versions.paradox.tasks,
      "net.codingwell" %% "scala-guice" % versions.scalaGuice,
      "org.postgresql" % "postgresql" % versions.postgres,
      "io.dropwizard.metrics" % "metrics-core" % versions.dropwizardMetrics,
      "io.dropwizard.metrics" % "metrics-graphite" % versions.dropwizardMetrics,
      "com.iheart" %% "ficus" % versions.ficus,
      "com.typesafe" % "config" % versions.typesafeConfig,
      "org.slf4j" % "jul-to-slf4j" % versions.julSlf4j,
      "org.slf4j" % "jcl-over-slf4j" % versions.julSlf4j,
      "org.codehaus.janino" % "janino" % versions.janino,
      "org.scalaj" %% "scalaj-http" % versions.scalajhttp,
      "ch.qos.logback" % "logback-classic" % versions.logback % "test",
      "io.paradoxical" % "docker-client" % versions.paradox.docker % "test",
      "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test",
      "io.paradoxical" %% "finatra-test" % versions.paradox.finatra % "test",
      "com.squareup.okhttp3" % "mockwebserver" % versions.okhttp % "test"
    ) ++ Dependencies.testDeps
  )
).dependsOn().
  enablePlugins(DockerPlugin)
