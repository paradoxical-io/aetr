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

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "io.paradoxical" %% "paradox-scala-jackson" % versions.paradox.global,
      "io.paradoxical" %% "finatra-server" % versions.paradox.finatra,
      "ch.qos.logback" % "logback-classic" % versions.logback % "test",
      "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test",
      "io.paradoxical" %% "finatra-test" % versions.paradox.finatra % "test"
    ) ++ Dependencies.testDeps
  )
).dependsOn().
  enablePlugins(DockerPlugin)
