package net.mojodna.metricsd

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import com.yammer.metrics.reporting.{ConsoleReporter, GraphiteReporter}
import net.mojodna.metricsd.server.{ManagementServer, MetricsServer}

class MetricsDaemon(config: Config) extends LazyLogging {
  def apply() = {

    if (config.getBoolean("debug")) {
      logger.info("Console Reporter enabled (debugging mode)")
      ConsoleReporter.enable(10, TimeUnit.SECONDS)
    }

    val flushInterval = config.getInt("graphite.flushInterval")
    val graphiteHost = config.getString("graphite.host")
    val graphitePort = config.getInt("graphite.port")
    logger.info(s"Flushing to graphite at $graphiteHost:$graphitePort every $flushInterval seconds")

    GraphiteReporter.enable(
      flushInterval,
      TimeUnit.SECONDS,
      graphiteHost,
      graphitePort
    )

    // TODO bootstrap with counter metrics to avoid resets when the service
    // restarts

    new MetricsServer(
      config.getInt("port"),
      config.getString("prefix")
    ).listen

    new ManagementServer(
      config.getInt("managementPort")
    ).listen
  }
}

object MetricsDaemon extends App with LazyLogging {
  try {
    val config = ConfigFactory.load()

    new MetricsDaemon(config)()
  } catch {
    case e: ConfigException => logger.error(s"Problem with configuration: ${e.getMessage}")
  }
}