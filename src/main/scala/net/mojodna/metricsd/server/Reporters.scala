package net.mojodna.metricsd.server

import java.net.InetSocketAddress

import com.codahale.metrics._
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.smartcodeltd.metrics.ForgetfulFilter
import com.typesafe.scalalogging.LazyLogging

trait Reporters extends LazyLogging {
  import java.util.concurrent.TimeUnit._

  def register(reporter: (MetricRegistry) => JmxReporter)(implicit metrics: MetricRegistry) = reporter(metrics)

  def schedule(reporter: (MetricRegistry) => ScheduledReporter)(implicit metrics: MetricRegistry): ScheduledReporter =
    reporter(metrics)

  // -- Auto-starting reporters

  def aJmxReporter(metrics: MetricRegistry)(implicit memorySpan: Long): JmxReporter = {
    logger.info("Reporting on JMX - enabled")

    JmxReporter.forRegistry(metrics)
      .convertRatesTo(SECONDS).convertDurationsTo(MILLISECONDS)
      .filter(new ForgetfulFilter(memorySpan))
      .build()
  }

  // -- Scheduled reporters
  def aConsoleReporter(metrics: MetricRegistry)(implicit memorySpan: Long): ScheduledReporter = {
    logger.info("Reporting to console - scheduled")

    ConsoleReporter.forRegistry(metrics)
      .convertRatesTo(SECONDS).convertDurationsTo(MILLISECONDS)
      .filter(new ForgetfulFilter(memorySpan))
      .build()
  }

  def aGraphiteReporter(address: InetSocketAddress, graphitePrefix: String)(metrics: MetricRegistry)(implicit memorySpan: Long): ScheduledReporter = {
    logger.info(s"Reporting to graphite (${ address.getHostName }:${ address.getPort }, prefix: '$graphitePrefix') - scheduled")

    GraphiteReporter.forRegistry(metrics)
      .prefixedWith(graphitePrefix)
      .convertRatesTo(SECONDS).convertDurationsTo(MILLISECONDS)
      .filter(new ForgetfulFilter(memorySpan))
      .build(new Graphite(address))
  }
}