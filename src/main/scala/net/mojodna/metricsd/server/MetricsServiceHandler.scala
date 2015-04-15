package net.mojodna.metricsd.server

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import com.yammer.metrics.Metrics
import com.yammer.metrics.core.MetricName
import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent, MessageEvent, SimpleChannelUpstreamHandler}

import scala.math.round
import scala.util.matching.Regex

/**
 * A service handler for :-delimited metrics strings (à la Etsy's statsd).
 */
class MetricsServiceHandler(prefix: String)
  extends SimpleChannelUpstreamHandler with LazyLogging {

  val COUNTER_METRIC_TYPE = "c"
  val GAUGE_METRIC_TYPE = "g"
  val HISTOGRAM_METRIC_TYPE = "h"
  val METER_METRIC_TYPE = "m"
  val METER_VALUE_METRIC_TYPE = "mn"
  val TIMER_METRIC_TYPE = "ms"

  val MetricMatcher = new Regex("""([^:]+)(:((-?\d+|delete)?(\|((\w+)(\|@(\d+\.\d+))?)?)?)?)?""")

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val msg = e.getMessage.asInstanceOf[String]

    logger.trace(s"Received message: $msg")

    msg.trim.split("\n").foreach {
      line: String =>
        // parse message
        val MetricMatcher(_metricName, _, _, _value, _, _, _metricType, _, _sampleRate) = line

        // clean up the metric name
        val metricName = _metricName.replaceAll("\\s+", "_").replaceAll("\\/", "-").replaceAll("[^a-zA-Z_\\-0-9\\.]", "")

        val metricType = if ((_value == null || _value.equals("delete")) && _metricType == null) {
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

        if (deleteMetric) {
          val name: MetricName = metricType match {
            case COUNTER_METRIC_TYPE =>
              new MetricName("metrics", "counter", metricName)

            case GAUGE_METRIC_TYPE =>
              new MetricName("metrics", "gauge", metricName)

            case HISTOGRAM_METRIC_TYPE | TIMER_METRIC_TYPE =>
              new MetricName("metrics", "histogram", metricName)

            case METER_METRIC_TYPE | METER_VALUE_METRIC_TYPE =>
              new MetricName("metrics", "meter", metricName)
          }

          logger.debug(s"Deleting metric '$name'")
          Metrics.defaultRegistry.removeMetric(name)
        } else {
          metricType match {
            case COUNTER_METRIC_TYPE =>
              logger.debug(s"Incrementing counter '$metricName' with $value at sample rate $sampleRate (${ round(value * 1 / sampleRate) })")
              Metrics.newCounter(new MetricName("metrics", "counter", metricName)).inc(round(value * 1 / sampleRate))

            case GAUGE_METRIC_TYPE =>
              logger.debug(s"Updating gauge '$metricName' with $value")
              // use a counter to simulate a gauge
              val counter = Metrics.newCounter(new MetricName("metrics", "gauge", metricName))
              counter.clear()
              counter.inc(value)

            case HISTOGRAM_METRIC_TYPE | TIMER_METRIC_TYPE =>
              logger.debug(s"Updating histogram '$metricName' with $value")
              // note: assumes that values have been normalized to integers
              Metrics.newHistogram(new MetricName("metrics", "histogram", metricName), true).update(value)

            case METER_METRIC_TYPE =>
              logger.debug(s"Marking meter '$metricName'")
              Metrics.newMeter(new MetricName("metrics", "meter", metricName), "samples", TimeUnit.SECONDS).mark()

            case METER_VALUE_METRIC_TYPE =>
              logger.debug(s"Marking meter '$metricName' with $value")
              Metrics.newMeter(new MetricName("metrics", "meter", metricName), "samples", TimeUnit.SECONDS).mark(value)

            case x: String =>
              logger.error(s"Unknown metric type: $x")
          }

          Metrics.newMeter(new MetricName(prefix, "meter", "samples"), "samples", TimeUnit.SECONDS).mark()
        }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("Exception in MetricsServiceHandler", e)
  }
}