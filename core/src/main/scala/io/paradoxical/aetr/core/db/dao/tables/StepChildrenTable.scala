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

object StepChildren {
  val TableName = "step_children"
}

class StepChildren @Inject()(
  val steps: Steps,
  val driver: JdbcProfile,
  dataMappers: DataMappers
) extends SlickDAO {

  import dataMappers._
  import driver.api._

  override type RowType = StepChildrenDao
  override type TableType = StepChildTable

  class StepChildTable(tag: Tag) extends DAOTable(tag, "step_children") {
    def id = column[StepTreeId]("id")

    def childOrder = column[Long]("child_order")

    def childId = column[StepTreeId]("child_id")

    def pk = primaryKey("pk_id_order_child", (id, childOrder, childId))

    def idFk = foreignKey("step_children_id_steps_id_fk", id, steps.query)(_.id)
    def childIdFk = foreignKey("step_children_child_id_steps_id_fk", childId, steps.query)(_.id)

    override def * =
      (
        id,
        childOrder,
        childId
      ) <> (StepChildrenDao.tupled, StepChildrenDao.unapply)
  }

  override val query = TableQuery[StepChildTable]
}
