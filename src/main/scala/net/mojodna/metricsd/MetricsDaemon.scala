package net.mojodna.metricsd

import java.util.concurrent.TimeUnit

import com.codahale.logula.Logging
import com.typesafe.config.{ConfigException, Config, ConfigFactory}
import com.yammer.metrics.reporting.{ConsoleReporter, GraphiteReporter}
import net.mojodna.metricsd.server.{ManagementServer, MetricsServer}
import org.apache.log4j.Level

class MetricsDaemon(config: Config) extends Logging {
  def apply() = {

    if (config.getBoolean("debug")) {
      ConsoleReporter.enable(10, TimeUnit.SECONDS)
    }

    val flushInterval = config.getInt("graphite.flushInterval")
    val graphiteHost = config.getString("graphite.host")
    val graphitePort = config.getInt("graphite.port")
    log.info("Flushing to %s:%d every %ds", graphiteHost, graphitePort, flushInterval)

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

object MetricsDaemon extends App {
  try {
    val config = ConfigFactory.load()

    Logging.configure {
      log =>
        log.registerWithJMX = true

        log.level = Level.toLevel(config.getString("log.level"))

        log.file.enabled = true
        log.file.filename = config.getString("log.file")
        log.file.maxSize = 10 * 1024
        log.file.retainedFiles = 5
    }

    new MetricsDaemon(config)()

  } catch {
    case e: ConfigException => println(e.getMessage)
  }
}