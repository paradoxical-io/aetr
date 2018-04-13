package io.paradoxical.aetr.core.lifecycle

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.graphite.{GraphiteReporter, GraphiteUDP}
import io.paradoxical.aetr.core.config.ServiceConfig
import io.paradoxical.aetr.core.db.dao.StepDb
import io.paradoxical.aetr.core.execution.{AdvanceQueuer, Advancer}
import io.paradoxical.aetr.core.model.RunState
import io.paradoxical.aetr.core.stats.FinagleStatsBridgeReceiver
import io.paradoxical.common.extensions.Extensions._
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * On startup try and advance whatever we can.
 *
 * If the service crashes, or has other transient errors
 * this should catch any orphaned failures
 */
class Startup @Inject()(
  stepsDb: StepDb,
  advanceQueuer: AdvanceQueuer,
  serviceConfig: ServiceConfig,
  advancer: Advancer
) {
  protected val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def start(): Unit = {
    setupMetrics()

    enqueuePending()

    startDequeueThread()

    startPollThread()

    logger.info("Dequeue and polling threads started")
  }

  def stop(): Unit = {
    logger.info("Stopping...")
  }

  private def enqueuePending(): Unit = {
    try {
      val pendingIds = stepsDb.findUnlockedRuns(RunState.Pending).waitForResult()

      if(pendingIds.nonEmpty) {
        logger.info(s"Processing ${pendingIds.size} pending requests")

        pendingIds.map(advanceQueuer.enqueue)
      }
    } catch {
      case ex: Exception =>
        logger.warn("Unable to enqueue pending!", ex)
    }
  }

  private def startPollThread(): Unit = {
    val dequeueThread = new Thread(() => {
      while (true) {
        try {
          enqueuePending()

          Thread.sleep(serviceConfig.pendingPollTime.toMillis)
        } catch {
          case ex: Exception =>
            logger.error("Unknown error during polling for pending!", ex)

            Thread.sleep(serviceConfig.pendingPollTime.toMillis)
        }
      }
    })

    dequeueThread.setDaemon(true)
    dequeueThread.setName("Pending-Polling-thread")
    dequeueThread.start()
  }

  private def startDequeueThread(): Unit = {
    val dequeueThread = new Thread(() => {
      while (true) {
        try {
          val dequeue = advanceQueuer.dequeue()

          advancer.advance(dequeue)
        } catch {
          case ex: Exception =>
            logger.error("Unknown error during advancement!", ex)

            Thread.sleep(serviceConfig.threadSleepOnDequeueError.toMillis)
        }
      }
    })

    dequeueThread.setDaemon(true)
    dequeueThread.setName("Dequeue-thread")
    dequeueThread.start()
  }

  private def setupMetrics(): Unit = {
    serviceConfig.stats.graphite.foreach(graphiteConfig => {
      logger.info(s"Starting Graphite stats reporter: ${graphiteConfig.host}:${graphiteConfig.port}")

      val sender = new GraphiteUDP(graphiteConfig.host, graphiteConfig.port)
      val reporter = GraphiteReporter.
        forRegistry(FinagleStatsBridgeReceiver.metrics).
        convertRatesTo(TimeUnit.MILLISECONDS).
        build(sender)
      reporter.start(graphiteConfig.interval.toSeconds, TimeUnit.SECONDS)
    })

    if (serviceConfig.stats.print_to_console) {
      logger.info("Starting console stats reporter")

      val reporter = ConsoleReporter.
        forRegistry(FinagleStatsBridgeReceiver.metrics).
        convertRatesTo(TimeUnit.MILLISECONDS).
        build()

      reporter.start(5, TimeUnit.SECONDS)
    }
  }
}
