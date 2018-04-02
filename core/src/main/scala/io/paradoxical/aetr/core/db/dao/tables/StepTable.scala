package io.paradoxical.aetr.core.db.dao.tables

import io.paradoxical.aetr.core.db.dao.DataMappers
import io.paradoxical.aetr.core.model._
import io.paradoxical.rdb.slick.dao.SlickDAO
import java.time.Instant
import javax.inject.Inject
import slick.jdbc.JdbcProfile

case class StepTreeDao(
  id: StepTreeId,
  name: NodeName,
  stepType: StepType,
  root: Option[StepTreeId],
  execution: Option[Execution],
  createdAt: Instant = Instant.now(),
  lastUpdatedAt: Instant = Instant.now()
)

class Steps @Inject()()(val driver: JdbcProfile, dataMappers: DataMappers) extends SlickDAO {

  import dataMappers._
  import driver.api._

  override type RowType = StepTreeDao
  override type TableType = StepsTable

  class StepsTable(tag: Tag) extends DAOTable(tag, "steps") {
    def id = column[StepTreeId]("id", O.PrimaryKey)

    def name = column[NodeName]("name")

    def stepType = column[StepType]("type")

    def root = column[Option[StepTreeId]]("root_id")

    def execution = column[Option[Execution]]("execution")

    def createdAt = column[Instant]("created_at")

    def lastUpdatedAt = column[Instant]("updated_at")

    override def * =
      (
        id,
        name,
        stepType,
        root,
        execution,
        createdAt,
        lastUpdatedAt
      ) <> (StepTreeDao.tupled, StepTreeDao.unapply)
  }

  override val query = TableQuery[StepsTable]
}

