package net.mojodna.metricsd.server

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.LazyLogging
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.channel.{ChannelPipeline, ChannelPipelineFactory, Channels}
import org.jboss.netty.handler.codec.frame.{DelimiterBasedFrameDecoder, Delimiters}
import org.jboss.netty.handler.codec.string.{StringDecoder, StringEncoder}
import org.jboss.netty.util.CharsetUtil

class ManagementServer(metrics: MetricRegistry, port: Int) extends LazyLogging {
  def listen = {

    val b = new ServerBootstrap(
      new NioServerSocketChannelFactory(
        Executors.newCachedThreadPool,
        Executors.newCachedThreadPool
      )
    )

    b.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline: ChannelPipeline = {
        val pipeline = Channels.pipeline

        pipeline.addLast(
          "framer",
          new DelimiterBasedFrameDecoder(
            512, //Geez how big a command do we expect
            Delimiters.lineDelimiter:_*
          )
        )
        pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8))
        pipeline.addLast("handler", new ManagementServiceHandler(metrics))

        pipeline
      }
    })

    logger.info(s"Listening on port $port.")
    b.bind(new InetSocketAddress(port))
  }
}