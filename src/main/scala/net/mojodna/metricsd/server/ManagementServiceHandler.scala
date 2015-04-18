package net.mojodna.metricsd.server

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.LazyLogging
import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent, MessageEvent, SimpleChannelUpstreamHandler}

import scala.collection.JavaConversions._

class ManagementServiceHandler(metrics: MetricRegistry)
  extends SimpleChannelUpstreamHandler with LazyLogging {

  val HELP = "help"
  val COUNTERS = "counters"
  val GAUGES = "gauges"
  val HISTOGRAMS = "histograms"
  val TIMERS = "timers"
  val METERS = "meters"
  val QUIT = "quit"

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val msg = e.getMessage.asInstanceOf[String]

    logger.trace(s"Received message: $msg")

    msg match {
      case HELP =>
        e.getChannel.write("COMMANDS: counters, gauges, histograms, timers, meters, quit\n\n")
      case COUNTERS =>
        for((metricName, metric) <- metrics.getCounters if ! metricName.startsWith("metrics.gauges")) e.getChannel.write(metricName + "\n")
        e.getChannel.write("END\n\n")
      case GAUGES =>
        for((metricName, metric) <- metrics.getCounters if metricName.startsWith("metrics.gauges")) e.getChannel.write(metricName + "\n")
        e.getChannel.write("END\n\n")
      case HISTOGRAMS =>
        for((metricName, metric) <- metrics.getHistograms) e.getChannel.write(metricName + "\n")
        e.getChannel.write("END\n\n")
      case TIMERS =>
        for((metricName, metric) <- metrics.getTimers) e.getChannel.write(metricName + "\n")
        e.getChannel.write("END\n\n")
      case METERS =>
        for((metricName, metric) <- metrics.getMeters) e.getChannel.write(metricName + "\n")
        e.getChannel.write("END\n\n")
      case QUIT =>
        e.getChannel.close
      case x: String =>
        logger.error("Unknown command: %s", x)
        e.getChannel.write("Unknown command\n")
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("Exception in ManagementServiceHandler", e)
  }
}