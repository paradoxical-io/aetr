import BuildConfig._
import sbtdocker.DockerKeys.imageName
import sbtdocker.ImageName

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
      val artifactTargetPath = s"/app/bin/${artifact.name}"

      new Dockerfile {
        from("openjdk:8u162-jre-slim")
        add(baseDirectory.value / "src/docker/", "/app")
        add(artifact, artifactTargetPath)
        runRaw("chmod +x /app/main.sh")
        entryPoint("/app/main.sh")
      }
    },

    (imageNames in docker) := Seq(
      ImageName(
        namespace = Some("paradoxical"),
        tag = Some(Revision.version),
        repository = "aetr"
      )
    ),

    // keep the log level quiet
    logLevel in assembly := Level.Error,

    // how we built our fat jar. lot of this is default from the fat jar plugin
    // with tweaks to ignore node modules
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) =>
        xs map {_.toLowerCase} match {
          case (x :: Nil) if Seq("manifest.mf", "index.list", "dependencies") contains x =>
            MergeStrategy.discard
          case ps @ (x :: _) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") || ps.last.endsWith(".rsa") =>
            MergeStrategy.discard
          case "maven" :: _ =>
            MergeStrategy.discard
          case "plexus" :: _ =>
            MergeStrategy.discard
          case "services" :: _ =>
            MergeStrategy.filterDistinctLines
          case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) | ("spring.tooling" :: Nil) =>
            MergeStrategy.filterDistinctLines
          case _ => MergeStrategy.first
        }
      case x if Assembly.isConfigFile(x) =>
        MergeStrategy.concat
      case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
        MergeStrategy.rename
      case PathList(ps @ _*) if Assembly.isSystemJunkFile(ps.last) =>
        MergeStrategy.discard
      case PathList(ps @ _*) if ps.contains("node_modules") =>
        MergeStrategy.discard
      case _ => MergeStrategy.first
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

addCommandAlias("all", ";compile ;ui:compile ;test ;assembly ;docker")
