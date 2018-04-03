package io.paradoxical.aetr.core.stats

import com.codahale.metrics.MetricRegistry.MetricSupplier
import com.codahale.metrics.{Metric, MetricFilter, MetricRegistry, Gauge => CodehaleGauge}
import com.twitter.finagle.stats._

object FinagleStatsBridgeReceiver {
  private[this] var _metrics = new MetricRegistry

  // Do we even need this
  def setMetricRegistry(registry: MetricRegistry): Unit = {
    synchronized {
      _metrics = registry
    }
  }

  def metrics = _metrics
}

class FinagleStatsBridgeReceiver extends StatsReceiver with StatsReceiverWithCumulativeGauges {
  import FinagleStatsBridgeReceiver._

  override def repr: AnyRef = this

  override def counter(verbosity: Verbosity, name: String*): Counter = {
    CounterImpl(formatKey(name))
  }

  override def stat(verbosity: Verbosity, name: String*): Stat = {
    StatImpl(formatKey(name))
  }

  override protected[this] def registerGauge(verbosity: Verbosity, name: Seq[String], f: => Float): Unit = {
    metrics.gauge(formatKey(name), new MetricSupplier[CodehaleGauge[_]] {
      override def newMetric(): CodehaleGauge[_] = new CodehaleGauge[Float] {
        override def getValue: Float = f
      }
    })
  }

  override protected[this] def deregisterGauge(name: Seq[String]): Unit = {
    removeKeyFromRegistry(formatKey(name))
  }

  protected def formatKey(key: Seq[String]): String = key.mkString(".")

  protected def removeKeyFromRegistry(key: String): Unit = {
    metrics.removeMatching(new MetricFilter {
      override def matches(name: String, metric: Metric): Boolean = name == key
    })
  }

  private case class GaugeImpl(name: String, f: () => Float) extends CodehaleGauge[Float] {
    override def getValue: Float = f()
  }

  private case class CounterImpl(name: String) extends Counter {
    private val underlying = metrics.counter(name)
    override def incr(delta: Long): Unit = underlying.inc(delta)
  }

  private case class StatImpl(name: String) extends Stat {
    private val underlying = metrics.histogram(name)
    override def add(value: Float): Unit = underlying.update(math.round(value)) // eek
  }
}

class MetricsCollisionException(message: String) extends IllegalStateException(message)