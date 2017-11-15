package io.paradoxical.aetr.global.tiny

import java.util.UUID

sealed trait ValueType[T] {
  self: Product =>

  val value: T

  def asString: String = value.toString

  override def toString: String = asString
}

trait StringValue extends ValueType[String] {
  self: Product =>
}
trait LongValue extends ValueType[Long] {
  self: Product =>
}
trait IntValue extends ValueType[Int] {
  self: Product =>
}
trait DoubleValue extends ValueType[Double] {
  self: Product =>
}
trait FloatValue extends ValueType[Float] {
  self: Product =>
}
trait UuidValue extends ValueType[UUID] {
  self: Product =>
}

