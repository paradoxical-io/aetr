package io.paradoxical.aetr.core.db

import io.paradoxical.rdb.slick.executors.CustomAsyncExecutor
import io.paradoxical.rdb.slick.providers.SlickDBProvider
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcProfile

class PostgresDbProvider @Inject()(
  val dataSource: javax.sql.DataSource
)(implicit executionContext: ExecutionContext) extends SlickDBProvider {
  override val driver: JdbcProfile = slick.jdbc.PostgresProfile

  import driver.api._

  protected lazy val db = Database.forDataSource(
    dataSource, None, executor = CustomAsyncExecutor(executionContext)
  )

  override def getDB: Database = db
}
