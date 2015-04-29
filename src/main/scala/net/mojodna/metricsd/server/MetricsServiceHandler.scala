package net.mojodna.metricsd.server

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.smartcodeltd.statsd._
import com.typesafe.scalalogging.LazyLogging
import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent, MessageEvent, SimpleChannelUpstreamHandler}

import scala.math.round
import scala.util.{Failure, Success}
/**
 * A service handler for :-delimited metrics strings (à la Etsy's statsd).
 */
class MetricsServiceHandler(metrics: MetricRegistry)
  extends SimpleChannelUpstreamHandler with LazyLogging {

  import MetricRegistry.{name => prefixed}
  import com.smartcodeltd.statsd.Parser.parse

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val msg = e.getMessage.asInstanceOf[String]

    logger.trace(s"Received message: $msg")
    metrics.meter(prefixed("metricsd", "meter", "packets")).mark()

    msg.trim.split("\n").map(parse).foreach ({ result =>
      result.map(debugLog)

      result match {
        case Success(sample) => record(sample)
        case Failure(error) =>
          logger.warn(error.getMessage)
          metrics.meter(prefixed("metricsd", "meter", "invalid_samples")).mark()
      }

      metrics.meter(prefixed("metricsd", "meter", "samples")).mark()
    })
  }

  // -- private

  private def record(sample: Sample): Unit = sample match {
    case GaugeSample(name, value) =>            // TODO: use metrics.gauge(full(metricName)).set(value) instead
      val counter = metrics.counter(fullNameOf(sample))
      counter.dec(counter.getCount)
      counter.inc(value)
    case CounterSample(name, value, sampleRate) => metrics.counter(fullNameOf(sample)).inc(round(value * 1 / sampleRate))
    case TimerSample(name, value)               => metrics.timer(fullNameOf(sample)).update(value, TimeUnit.MILLISECONDS)
    case HistogramSample(name, value)           => metrics.histogram(fullNameOf(sample)).update(value)
    case MeterSample(name, value)               => metrics.meter(fullNameOf(sample)).mark(value)
  }

  private def debugLog(sample: Sample): Unit = sample match {
      case GaugeSample(name, value)               => logger.debug(s"Updating gauge '${ fullNameOf(sample) }' with ${ value }")
      case CounterSample(name, value, sampleRate) => logger.debug(s"Incrementing counter '${ fullNameOf(sample) }' with $value at sample rate ${ sampleRate } (${ round(value * 1 / sampleRate) })")
      case TimerSample(name, value)               => logger.debug(s"Updating timer '${ fullNameOf(sample) }' with ${ value }")
      case HistogramSample(name, value)           => logger.debug(s"Updating histogram '${ fullNameOf(sample) }' with ${ value }")
      case MeterSample(name, value)               => logger.debug(s"Marking meter '${ fullNameOf(sample) }' with ${ value }")
    }

  private def fullNameOf(sample: Sample): String = sample match {
    case GaugeSample(name, value)               => prefixed("gauge", name)
    case CounterSample(name, value, sampleRate) => prefixed(name)
    case TimerSample(name, value)               => prefixed("timer", name)
    case HistogramSample(name, value)           => prefixed("histogram", name)
    case MeterSample(name, value)               => prefixed("meter", name)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("Exception in MetricsServiceHandler", e)
  }
}