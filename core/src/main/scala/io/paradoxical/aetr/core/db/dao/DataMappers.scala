package io.paradoxical.aetr.core.db.dao

import io.paradoxical.aetr.core.db.dao.tables.LockId
import io.paradoxical.aetr.core.model._
import io.paradoxical.jackson.JacksonSerializer
import io.paradoxical.rdb.slick.dao.SlickDAO
import io.paradoxical.rdb.slick.providers.SlickDBProvider
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import slick.ast.BaseTypedType
import slick.jdbc._

class DataMappers @Inject()(
  val slickDBProvider: SlickDBProvider,
  objectMapper: JacksonSerializer
) {

  import slickDBProvider.driver.api._

  lazy val converter = new SlickDAO.Implicits(slickDBProvider.driver)

  implicit val versionMapper: JdbcType[Version] with BaseTypedType[Version] = {
    MappedColumnType.base[Version, Long](_.value, Version)
  }

  implicit val stepTreeIdMapper: JdbcType[StepTreeId] with BaseTypedType[StepTreeId] = {
    MappedColumnType.base[StepTreeId, String](_.value.toString, s => StepTreeId(UUID.fromString(s)))
  }

  implicit val nodeNameMapper: JdbcType[NodeName] with BaseTypedType[NodeName] = {
    MappedColumnType.base[NodeName, String](_.value, NodeName)
  }

  implicit val runInstanceIdMapper: JdbcType[RunInstanceId] with BaseTypedType[RunInstanceId] = {
    MappedColumnType.base[RunInstanceId, String](_.value.toString, s => RunInstanceId(UUID.fromString(s)))
  }

  implicit val lockIdMapper: JdbcType[LockId] with BaseTypedType[LockId] = {
    MappedColumnType.base[LockId, String](_.value.toString, s => LockId(UUID.fromString(s)))
  }

  implicit val rootIdMapper: JdbcType[RootId] with BaseTypedType[RootId] = {
    MappedColumnType.base[RootId, String](_.value.toString, s => RootId(UUID.fromString(s)))
  }

  implicit val resultDataMapper: JdbcType[ResultData] with BaseTypedType[ResultData] = {
    MappedColumnType.base[ResultData, String](_.value, ResultData)
  }

  implicit val instantMapper: JdbcType[Instant] with BaseTypedType[Instant] = {
    MappedColumnType.base[Instant, Long](_.toEpochMilli, Instant.ofEpochMilli)
  }

  implicit val executionMapper: JdbcType[Execution] with BaseTypedType[Execution] = {
    MappedColumnType.base[Execution, String](objectMapper.toJson, objectMapper.fromJson[Execution])
  }

  implicit val stepStateMapper: JdbcType[RunState] with BaseTypedType[RunState] = {
    MappedColumnType.base[RunState, String](_.toString, RunState.valueOf)
  }

  implicit val stepTypeMapper: JdbcType[StepType] with BaseTypedType[StepType] = {
    MappedColumnType.base[StepType, String](_.toString, StepType.valueOf)
  }

  implicit val stepTreeSetParam = new SetParameter[StepTreeId] {
    override def apply(v1: StepTreeId, v2: PositionedParameters): Unit = v2.setString(v1.value.toString)
  }

  implicit val stepTreeGetResult = new GetResult[StepTreeId] {
    override def apply(v1: PositionedResult): StepTreeId = StepTreeId(UUID.fromString(v1.nextString()))
  }
}
