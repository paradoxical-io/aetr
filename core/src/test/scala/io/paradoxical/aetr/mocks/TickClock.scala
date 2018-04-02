package io.paradoxical.aetr.mocks

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.duration.FiniteDuration

class TickClock extends Clock {
  private var now = Instant.now()

  def tick(duration: FiniteDuration): Unit = {
    now = now.plusMillis(duration.toMillis)
  }

  override def withZone(zone: ZoneId): Clock = ???

  override def getZone: ZoneId = ???

  override def instant(): Instant = now
}
