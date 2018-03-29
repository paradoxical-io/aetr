package io.paradoxical.aetr.core.model

sealed trait Mapper {
  def map(in: ResultData): ResultData
}

object Mapper {
  val identity: Mapper =
    new Mapper {
      override def map(in: ResultData): ResultData = in
    }

  def fromFunc(f: ResultData => ResultData): Mapper =
    new Mapper {
      override def map(in: ResultData): ResultData = f(in)
    }
}
