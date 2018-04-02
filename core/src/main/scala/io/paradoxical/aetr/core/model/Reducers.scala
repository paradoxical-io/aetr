package io.paradoxical.aetr.core.model

sealed trait Reducer {
  def reduce(ins: Seq[ResultData]): Option[ResultData]
}

object Reducer {
  val noop =
    new Reducer {
      override def reduce(ins: Seq[ResultData]): Option[ResultData] = None
    }

  val last = new Reducer {
    override def reduce(ins: Seq[ResultData]): Option[ResultData] = ins.lastOption
  }

  def fromFunc(f: Seq[ResultData] => Option[ResultData]): Reducer =
    new Reducer {
      override def reduce(ins: Seq[ResultData]): Option[ResultData] = f(ins)
    }
}
