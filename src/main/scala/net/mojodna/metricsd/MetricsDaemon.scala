package net.mojodna.metricsd

import java.net.InetSocketAddress

import com.codahale.metrics.MetricRegistry
import com.smartcodeltd.statsd.TimestampingMetricRegistry
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import net.mojodna.metricsd.server.{ManagementServer, MetricsServer, Reporters}


class MetricsDaemon(config: Config) extends LazyLogging with Reporters {
  import java.util.concurrent.TimeUnit._

  implicit private val metrics: MetricRegistry = new TimestampingMetricRegistry()
  implicit private val memorySpan = config.getLong("graphite.flushInterval")

  private val metricsServer    = new MetricsServer(metrics, config.getInt("port"))

  private val managementServer = new ManagementServer(metrics, config.getInt("managementPort"))
  private val prefix  = config.getString("prefix")
  private val address = new InetSocketAddress(config.getString("graphite.host"), config.getInt("graphite.port"))

  private val scheduledReporters =
    schedule(aGraphiteReporter(address, prefix)) ::
    ifDebug(schedule(aConsoleReporter))

  private val jmx = register(aJmxReporter)


  def start(): Unit = {
    val flushInterval = config.getInt("graphite.flushInterval")

    logger.info(s"Flushing metrics every $flushInterval seconds")

    metricsServer.listen
    managementServer.listen

    jmx.start()

    scheduledReporters.foreach(_.start(flushInterval, SECONDS))
  }

  def stop(): Unit = {

    logger.info("Shutting down...")

    jmx.stop()

    scheduledReporters.foreach { reporter =>
      reporter.report()
      reporter.stop()
    }

    logger.info("Done. Have a nice day :-)")
  }

  // -- private
  private def ifDebug[T](x: => T): List[T] = if (config.getBoolean("debug")) List(x) else Nil
}

object MetricsDaemon extends App with LazyLogging {
  try {
    val daemon = new MetricsDaemon(ConfigFactory.load())
    
    daemon.start()

    sys.addShutdownHook({ daemon.stop() })
  } catch {
    case e: ConfigException => logger.error(s"Problem with configuration: ${e.getMessage}")
  }
}