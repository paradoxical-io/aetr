package io.paradoxical.aetr

import io.paradoxical.aetr.core.model.{Mappers, Reducers, ResultData}

class NashornTests extends TestBase {

  "Nashorn mapper" should "invoke" in {
    Mappers.Nashorn(
      """
        |function apply(data) {
        |   return data + "foo";
        |}
      """.stripMargin).map(ResultData("bar")) shouldEqual ResultData("barfoo")
  }

  it should "fail when accessing java types" in {
    assertThrows[Exception] {
      Mappers.Nashorn(
        """
          |function apply(data) {
          |   var HashMap = Java.type("java.util.HashMap")
          |}
        """.stripMargin).map(ResultData("bar"))
    }
  }

  it should "access object data if its structured" in {
    val js =   """
                 |function apply(data) {
                 |   var parsed = JSON.parse(data);
                 |
                 |   parsed.name = parsed.name + "foo";
                 |
                 |   return JSON.stringify(parsed);
                 |}
               """.stripMargin

    Mappers.Nashorn(js).map(ResultData(
      """{
        | "name" : "butts"
        | }
      """.stripMargin)).value shouldEqual """{"name":"buttsfoo"}"""
  }

  "Nashorn reducer" should "reduce" in {
    Reducers.Nashorn(
      """
        |function apply(data) {
        |   return data[0] + "foo"
        |}
      """.stripMargin).reduce(List(ResultData("bar"))) shouldEqual Some(ResultData("barfoo"))
  }

  it should "join items" in {
    Reducers.Nashorn(
      """
        |function apply(data) {
        |   return data.join("")
        |}
      """.stripMargin).reduce(List(ResultData("bar"), ResultData("baz"))) shouldEqual Some(ResultData("barbaz"))
  }

  it should "return None if null is returned" in {
    Reducers.Nashorn(
      """
        |function apply(data) {
        |   return null;
        |}
      """.stripMargin).reduce(List(ResultData("bar"))) shouldEqual None
  }
}
