package io.paradoxical.aetr.db

import io.paradoxical.v2.{Container, DockerCreator}
import io.paradoxical.{DockerClientConfig, EnvironmentVar}
import java.sql.{Connection, DriverManager}
import org.joda.time.DateTime
import scala.collection.JavaConverters._

object Postgres {
  def docker(): PostgresDocker = {
    val config = DockerClientConfig.
      builder().
      imageName("postgres:10.3-alpine").
      pullAlways(true).
      envVars(
        List(
          new EnvironmentVar("POSTGRES_PASSWORD", "test"),
          new EnvironmentVar("POSTGRES_USER", "test")
        ).asJava
      ).
      port(5432).
      build()

    val container = DockerCreator.build(config)

    val port = container.getTargetPortToHostPortLookup.get(5432)

    val postgres = PostgresDocker(container, port, "test", "test")

    val expiration = DateTime.now().plusSeconds(60)

    while (DateTime.now().isBefore(expiration) && !postgres.isOpen) {
      Thread.sleep(100)
    }

    postgres
  }
}

case class PostgresDocker(container: Container, port: Int, user: String, password: String) {
  Class.forName("org.postgresql.Driver")

  def close() = container.getClient().killContainerCmd(container.getContainerInfo.getId).exec()

  def isOpen: Boolean = {
    try {
      connect(_.close())

      true
    } catch {
      case _: Throwable =>
        false
    }
  }

  def jdbc(db: String = ""): String = {
    s"${url(db)}?user=$user&password=$password"
  }

  def url(db: String = ""): String = {
    s"jdbc:postgresql://localhost:$port/$db"
  }

  def createDatabase(db: String): String = {
    connect { conn =>
      conn.createStatement().execute(makeDatabaseString(db))
    }

    jdbc(db)
  }

  private def makeDatabaseString(db: String): String = {
    s"CREATE DATABASE $db WITH OWNER = $user"
  }

  def dropDatabase(db: String): String = {
    connect { conn =>
      conn.createStatement().execute(s"drop database if exists ${db};")
    }

    jdbc(db)
  }

  private def connect[T](block: Connection => T): T = {
    block(DriverManager.getConnection(jdbc()))
  }

  def connectWith[T](jdbc: String)(block: Connection => T): T = {
    block(DriverManager.getConnection(jdbc))
  }
}