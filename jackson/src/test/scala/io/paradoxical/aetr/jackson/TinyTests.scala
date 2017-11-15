package io.paradoxical.aetr.jackson

import io.paradoxical.aetr.global.tiny._
import java.util.UUID
import org.scalatest.{FlatSpec, Matchers}

case class TestLong(value: Long) extends LongValue
case class TestString(value: String) extends StringValue
case class TestUUID(value: UUID) extends UuidValue
case class TestFloat(value: Float) extends FloatValue
case class TestDouble(value: Double) extends DoubleValue

case class IdHolder(yes: TestLong, opt: Option[TestLong])

class TinyTests extends FlatSpec with Matchers {
  val jackson = new JacksonSerializer()

  "Tiny type" should "serialize" in {
    val uuid = UUID.randomUUID()

    jackson.toJson(TestLong(1)) shouldEqual "1"
    jackson.toJson(TestString("abc")) shouldEqual "\"abc\""
    jackson.toJson(TestUUID(uuid)).replace("\"", "") shouldEqual uuid.toString
    jackson.toJson(TestFloat(1.23F)) shouldEqual "1.23"
    jackson.toJson(TestDouble(1.234D)) shouldEqual "1.234"
  }

  it should "deserialize options" in {
    jackson.fromJson[Option[TestLong]]("1") shouldEqual Some(TestLong(1))
  }

  it should "deserialize containers" in {
    jackson.fromJson[IdHolder](
      """
        | {
        |  "yes": 1,
        |  "opt": 2
        |  }
      """.stripMargin) shouldEqual IdHolder(TestLong(1), Some(TestLong(2)))
  }

  it should "serialize containers" in {
    jackson.toJson[IdHolder](IdHolder(TestLong(1), Some(TestLong(2)))) shouldEqual """{"yes":1,"opt":2}"""
  }

  it should "serialize to map values" in {
    jackson.fromJson[Map[String, TestLong]](
      """
        |{ "foo": 1 }
      """.stripMargin) shouldEqual Map(
      "foo" -> TestLong(1)
    )
  }

  it should "deserialize from map values" in {
    jackson.fromJson[Map[TestLong, TestLong]](
      """
        |{ "1": 1 }
      """.stripMargin) shouldEqual Map(
      TestLong(1) -> TestLong(1)
    )
  }
}
