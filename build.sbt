import BuildConfig._

lazy val commonSettings = BuildConfig.commonSettings(currentVersion = "1.1")

/**
 * The project root
 */
lazy val `aetr` = project.
  in(file(".")).
  settings(commonSettings).
  settings(
    publish := {},
    aggregate in update := false
  ).aggregate(core, global, jackson, model)

lazy val global = project.settings(commonSettings).settings(
  Seq(
    name := "aetr-global",

    libraryDependencies ++= Seq() ++ Dependencies.testDeps
  )
)

lazy val jackson = project.settings(commonSettings).settings(
  Seq(
    name := "aetr-jackson",

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.1"
    ) ++ Dependencies.testDeps
  )
).dependsOn(global)

lazy val model = project.settings(commonSettings).settings(
  Seq(
    name := "aetr-model",

    libraryDependencies ++= Seq() ++ Dependencies.testDeps
  )
).dependsOn(global)

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
      "com.twitter" %% "finatra-http" % versions.finatra,
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "ch.qos.logback" % "logback-classic" % versions.logback % "test",

      "com.twitter" %% "finatra-http" % versions.finatra % "test",
      "com.twitter" %% "inject-server" % versions.finatra % "test",
      "com.twitter" %% "inject-app" % versions.finatra % "test",
      "com.twitter" %% "inject-core" % versions.finatra % "test",
      "com.twitter" %% "inject-modules" % versions.finatra % "test",
      "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test",

      "com.twitter" %% "finatra-http" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-server" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-app" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-core" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-modules" % versions.finatra % "test" classifier "tests",
    ) ++ Dependencies.testDeps
  )
).dependsOn(jackson, global).
  enablePlugins(DockerPlugin)

pgpPassphrase := Some(sys.env.getOrElse("GPG_PASSWORD", default = "").toArray)