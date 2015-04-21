package net.mojodna.metricsd.server

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.LazyLogging
import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent, MessageEvent, SimpleChannelUpstreamHandler}

import scala.math.round
import scala.util.matching.Regex

/**
 * A service handler for :-delimited metrics strings (à la Etsy's statsd).
 */
class MetricsServiceHandler(metrics: MetricRegistry)
  extends SimpleChannelUpstreamHandler with LazyLogging {

  import MetricRegistry.name

  /*
  https://github.com/b/statsd_spec
  http://statsd.readthedocs.org/en/latest/types.html

  todo: add validation:
    Meters     <metric name>:<value>|m                  value: [0, 2^64)
    Gauges     <metric name>:<value>|g                  value: [0, 2^64)
    Counters   <metric name>:<value>|c[|@<sample rate>] value: (-2^64, 2^64-1)
    Timers     <metric name>:<value>|ms                 value: [0, 2^64-1)
    Histograms <metric name>:<value>|h                  value: [0, 2^64-1)

    note: handling values up to (2^64 - 1) requires Java8 as former implementations of Long only handle (-2^63, 2^63-1)

  todo: compare statsd_spec with etsy's statsd helper unit tests

   */

  val COUNTER_METRIC_TYPE = "c"
  val GAUGE_METRIC_TYPE = "g"
  val METER_METRIC_TYPE = "m"
  val HISTOGRAM_METRIC_TYPE = "h"
  val TIMER_METRIC_TYPE = "ms"

  val MetricMatcher = new Regex("""([^:]+)(:((-?\d+|delete)?(\|((\w+)(\|@(\d+\.\d+))?)?)?)?)?""")

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val msg = e.getMessage.asInstanceOf[String]

    logger.trace(s"Received message: $msg")

    msg.trim.split("\n").foreach {
      line: String =>
        // -- Parsing

        // parse message
        val MetricMatcher(_metricName, _, _, _value, _, _, _metricType, _, _sampleRate) = line

        // clean up the metric name
        val metricName = _metricName.replaceAll("\\s+", "_").replaceAll("\\/", "-").replaceAll("[^a-zA-Z_\\-0-9\\.]", "")

        implicit val metricType = if ((_value == null || _value.equals("delete")) && _metricType == null) {
          METER_METRIC_TYPE
        } else if (_metricType == null) {
          COUNTER_METRIC_TYPE
        } else {
          _metricType
        }

        val value: Long = if (_value != null && !_value.equals("delete")) {
          _value.toLong
        } else {
          1 // meaningless value
        }

        val deleteMetric = (_value != null && _value.equals("delete"))

        val sampleRate: Double = if (_sampleRate != null) {
          _sampleRate.toDouble
        } else {
          1.0
        }

        // -- Action

        if (deleteMetric) {

          logger.debug(s"Deleting metric '$metricName'")
          metrics.remove(by(full(metricName)))

        } else {
          metricType match {
            case COUNTER_METRIC_TYPE =>

              logger.debug(s"Incrementing counter '${ full(metricName) }' with $value at sample rate $sampleRate (${ round(value * 1 / sampleRate) })")
              metrics.counter(full(metricName)).inc(round(value * 1 / sampleRate))

            case GAUGE_METRIC_TYPE =>
              // todo: add support for gauge deltas:    <gauge_name>:[+-]<value>|g
              logger.debug(s"Updating gauge '${ full(metricName) }' with $value")

              // use a counter to simulate a gauge
              val counter = metrics.counter(full(metricName))
              counter.dec(counter.getCount)
              counter.inc(value)

            case HISTOGRAM_METRIC_TYPE =>

              logger.debug(s"Updating histogram '${ full(metricName) }' with $value")
              metrics.histogram(full(metricName)).update(value)

            case TIMER_METRIC_TYPE =>

              logger.debug(s"Updating timer '${ full(metricName) }' with $value")
              metrics.timer(full(metricName)).update(value, TimeUnit.MILLISECONDS)

            case METER_METRIC_TYPE =>

              logger.debug(s"Marking meter '${ full(metricName) }' with $value")
              metrics.meter(full(metricName)).mark(value)

            case x: String =>
              logger.error(s"Unknown metric type: $x")
          }

          // -- Internal stats

          metrics.meter(name("metricsd", "meter", "samples")).mark()
        }
    }
  }

  private def by[T](what: T):T = what

  private def full(metricName: String)(implicit metricType: String): String = metricType match {
    case COUNTER_METRIC_TYPE    => name(metricName)
    case GAUGE_METRIC_TYPE      => name("gauge", metricName)
    case HISTOGRAM_METRIC_TYPE  => name("histogram", metricName)
    case TIMER_METRIC_TYPE      => name("timer", metricName)
    case METER_METRIC_TYPE      => name("meter", metricName)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("Exception in MetricsServiceHandler", e)
  }
}