package net.mojodna.metricsd.server

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import com.typesafe.scalalogging.LazyLogging
import org.jboss.netty.bootstrap.ConnectionlessBootstrap
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory
import org.jboss.netty.channel.{ChannelPipeline, ChannelPipelineFactory, Channels, FixedReceiveBufferSizePredictorFactory}
import org.jboss.netty.handler.codec.string.{StringDecoder, StringEncoder}
import org.jboss.netty.util.CharsetUtil

class MetricsServer(port: Int, prefix: String) extends LazyLogging {
  def listen = {
    val f = new NioDatagramChannelFactory(Executors.newCachedThreadPool)

    val b = new ConnectionlessBootstrap(f)

    // Configure the pipeline factory.
    b.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline: ChannelPipeline = {
        Channels.pipeline(
          new StringEncoder(CharsetUtil.UTF_8),
          new StringDecoder(CharsetUtil.UTF_8),
          new MetricsServiceHandler(prefix)
        )
      }
    })

    b.setOption("broadcast", "false")

    b.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024))

    logger.info(s"Listening on port $port.")
    b.bind(new InetSocketAddress(port))
  }
}