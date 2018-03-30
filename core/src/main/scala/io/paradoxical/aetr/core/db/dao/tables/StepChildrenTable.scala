package io.paradoxical.aetr.core.db.dao.tables

import io.paradoxical.aetr.core.db.dao.DataMappers
import io.paradoxical.aetr.core.model.StepTreeId
import io.paradoxical.rdb.slick.dao.SlickDAO
import javax.inject.Inject
import slick.jdbc.JdbcProfile

case class StepChildrenDao(
  id: StepTreeId,
  childOrder: Long,
  childId: StepTreeId
)

class StepChildren @Inject()()(val driver: JdbcProfile, dataMappers: DataMappers) extends SlickDAO {

  import dataMappers._
  import driver.api._

  override type RowType = StepChildrenDao
  override type TableType = StepChildTable

  class StepChildTable(tag: Tag) extends DAOTable(tag, "step_children") {
    def id = column[StepTreeId]("id")

    def childOrder = column[Long]("child_order")

    def child = column[StepTreeId]("child_id")

    override def * =
      (
        id,
        childOrder,
        child
      ) <> (StepChildrenDao.tupled, StepChildrenDao.unapply)
  }

  override val query = TableQuery[StepChildTable]
}
