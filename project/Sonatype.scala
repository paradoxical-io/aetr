import sbt.Keys._
import sbt._

object Publishing {
  val publishSettings = Seq(
    publishMavenStyle := true,

    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),

    publishArtifact in Test := false,

    pomIncludeRepository := { _ => false },

// To sync with Maven central, you need to supply the following information:
    pomExtra := (
      <url>https://github.com/paradoxical-io/aetr</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          </license>
        </licenses>
        <scm>
          <connection>scm:git:github.com/paradoxical-io/aetr</connection>
          <developerConnection>scm:git:git@github.com:paradoxical-io/aetr.git</developerConnection>
          <url>github.com:paradoxical-io/aetr.git</url>
        </scm>
        <developers>
          <developer>
            <id>devshorts</id>
            <name>Anton Kropp</name>
          </developer>
        </developers>
      )
  )
}
