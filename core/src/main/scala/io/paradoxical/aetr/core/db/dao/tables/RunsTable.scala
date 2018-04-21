package io.paradoxical.aetr.core.db.dao.tables

import io.paradoxical.aetr.core.db.dao.DataMappers
import io.paradoxical.aetr.core.model._
import io.paradoxical.global.tiny.UuidValue
import io.paradoxical.rdb.slick.dao.SlickDAO
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import slick.jdbc.JdbcProfile

case class LockId(value: UUID) extends UuidValue

case class RunDao(
  id: RunInstanceId,
  root: RunInstanceId,
  parent: Option[RunInstanceId],
  version: Version,
  order: Long,
  stepTreeId: StepTreeId,
  state: RunState,
  input: Option[ResultData],
  output: Option[ResultData],
  createdAt: Instant,
  lastUpdatedAt: Instant,
  executedAt: Option[Instant],
  completedAt: Option[Instant],
  actionLockedTill: Option[Instant] = None,
  lockId: Option[LockId] = None
)

class Runs @Inject()()(val driver: JdbcProfile, dataMappers: DataMappers) extends SlickDAO {

  import dataMappers._
  import driver.api._

  override type RowType = RunDao
  override type TableType = RunTable

  class RunTable(tag: Tag) extends DAOTable(tag, "runs") {
    def id = column[RunInstanceId]("id", O.PrimaryKey)

    def root = column[RunInstanceId]("root_id")

    def parentId = column[Option[RunInstanceId]]("parent_id")

    def version = column[Version]("version")

    def order = column[Long]("order")

    def stepTreeId = column[StepTreeId]("step_id")

    def state = column[RunState]("state")

    def input = column[Option[ResultData]]("input")

    def output = column[Option[ResultData]]("output")

    def createdAt = column[Instant]("created_at")

    def lastUpdatedAt = column[Instant]("updated_at")

    def executedAt = column[Option[Instant]]("executed_at")

    def completedAt = column[Option[Instant]]("completed_at")

    def actionLockedTill = column[Option[Instant]]("action_locked_till")

    def lockId = column[Option[LockId]]("lock_id")

    override def * =
      (
        id,
        root,
        parentId,
        version,
        order,
        stepTreeId,
        state,
        input,
        output,
        createdAt,
        lastUpdatedAt,
        executedAt,
        completedAt,
        actionLockedTill,
        lockId
      ) <> (RunDao.tupled, RunDao.unapply)
  }

  override val query = TableQuery[RunTable]
}

