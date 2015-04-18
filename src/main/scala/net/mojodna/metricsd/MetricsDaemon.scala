package net.mojodna.metricsd

import java.net.InetSocketAddress

import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.codahale.metrics.{ConsoleReporter, MetricFilter, MetricRegistry, ScheduledReporter}
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import net.mojodna.metricsd.server.{ManagementServer, MetricsServer}


class MetricsDaemon(config: Config) extends LazyLogging {
  import java.util.concurrent.TimeUnit._

  private val metrics  = new MetricRegistry()

  // TODO bootstrap with counter metrics to avoid resets when the service restarts
  // ^- not sure what that means; maybe just flush the metrics on shutdown?
  private val metricsServer    = new MetricsServer(metrics, config.getInt("port"))
  private val managementServer = new ManagementServer(metrics, config.getInt("managementPort"))

  private val reporters = graphiteReporter :: ifDebug(consoleReporter)

  def start: Unit = {
    val flushInterval = config.getInt("graphite.flushInterval")
    logger.info(s"Flushing metrics every $flushInterval seconds")

    metricsServer.listen
    managementServer.listen

    reporters.foreach( r => r.start(flushInterval, SECONDS) )
  }

  // -- private

  private def graphiteReporter: ScheduledReporter = {
    val host   = config.getString("graphite.host")
    val port   = config.getInt("graphite.port")
    val prefix = config.getString("prefix")

    logger.info(s"Reporting to graphite at $host:$port with prefix '$prefix' - enabled")

    val graphite = new Graphite(new InetSocketAddress(host, port))

    GraphiteReporter.forRegistry(metrics)
      .prefixedWith(prefix)
      .convertRatesTo(SECONDS)
      .convertDurationsTo(MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphite)
  }

  private def consoleReporter: ScheduledReporter = {
    logger.info("Reporting to console - enabled")

    ConsoleReporter.forRegistry(metrics)
      .convertRatesTo(SECONDS)
      .convertDurationsTo(MILLISECONDS)
      .build()
  }

  private def ifDebug[T](x: => T): List[T] = if (config.getBoolean("debug")) List(x) else Nil
}

object MetricsDaemon extends App with LazyLogging {
  try {
    val config = ConfigFactory.load()

    new MetricsDaemon(config).start

    // todo: flush metrics on shutdown
  } catch {
    case e: ConfigException => logger.error(s"Problem with configuration: ${e.getMessage}")
  }
}