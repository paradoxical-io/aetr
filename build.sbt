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
      "io.paradoxical" %% "paradox-scala-util" % versions.paradox.global,
      "io.paradoxical" %% "slick" % versions.paradox.slick,
      "io.paradoxical" %% "finatra-server" % versions.paradox.finatra,
      "io.paradoxical" %% "finatra-swagger" % versions.paradox.finatra,
      "io.paradoxical" %% "tasks" % versions.paradox.tasks,
      "net.codingwell" %% "scala-guice" % versions.scalaGuice,
      "org.postgresql" % "postgresql" % versions.postgres,
      "com.iheart" %% "ficus" % "1.4.3",
      "com.typesafe" % "config" % "1.3.3",
      "org.slf4j" % "jul-to-slf4j" % "1.7.25",
      "org.codehaus.janino" % "janino" % "2.7.8",
      "org.slf4j" % "jcl-over-slf4j" % "1.7.25",
      "ch.qos.logback" % "logback-classic" % versions.logback % "test",
      "io.paradoxical" % "docker-client" % versions.paradox.docker % "test",
      "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test",
      "io.paradoxical" %% "finatra-test" % versions.paradox.finatra % "test"
    ) ++ Dependencies.testDeps
  )
).dependsOn().
  enablePlugins(DockerPlugin)
