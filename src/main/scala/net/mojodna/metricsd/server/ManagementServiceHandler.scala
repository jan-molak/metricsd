package net.mojodna.metricsd.server

import com.typesafe.scalalogging.LazyLogging
import com.yammer.metrics.Metrics
import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent, MessageEvent, SimpleChannelUpstreamHandler}

import scala.collection.JavaConversions._

class ManagementServiceHandler
  extends SimpleChannelUpstreamHandler with LazyLogging {

  val HELP = "help"
  val COUNTERS = "counters"
  val GAUGES = "gauges"
  val HISTOGRAMS = "histograms"
  val METERS = "meters"
  val QUIT = "quit"

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val msg = e.getMessage.asInstanceOf[String]

    logger.trace(s"Received message: $msg")

    msg match {
      case HELP =>
        e.getChannel.write("COMMANDS: counters, gauges, histograms, meters, quit\n\n")
      case COUNTERS =>
        for((metricName, metric) <- Metrics.defaultRegistry.allMetrics if metricName.getType == "counter") e.getChannel.write(metricName.getName + "\n")
        e.getChannel.write("END\n\n")
      case GAUGES =>
        for((metricName, metric) <- Metrics.defaultRegistry.allMetrics if metricName.getType == "gauge") e.getChannel.write(metricName.getName + "\n")
        e.getChannel.write("END\n\n")
      case HISTOGRAMS =>
        for((metricName, metric) <- Metrics.defaultRegistry.allMetrics if metricName.getType == "histogram") e.getChannel.write(metricName.getName + "\n")
        e.getChannel.write("END\n\n")
      case METERS =>
        for((metricName, metric) <- Metrics.defaultRegistry.allMetrics if metricName.getType == "meter") e.getChannel.write(metricName.getName + "\n")
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