package io.wasted.spdydemo

import io.netty.bootstrap._
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.SocketChannel

import scala.util.Try
import io.netty.handler.ssl.SslHandler
import scala.util.Failure
import scala.Some
import scala.util.Success
import org.eclipse.jetty.npn.NextProtoNego
import java.security.KeyStore
import javax.net.ssl.{ SSLContext, KeyManagerFactory }

object SpdyDemo extends App { PS =>
  private var eventLoop1: Option[NioEventLoopGroup] = None
  private var eventLoop2: Option[NioEventLoopGroup] = None

  override def main(args: Array[String]): Unit = start()

  def start() {
    eventLoop1 = Some(new NioEventLoopGroup)
    eventLoop2 = Some(new NioEventLoopGroup)

    val keystore = KeyStore.getInstance("JKS")
    keystore.load(BogusKeyStore.asInputStream(), BogusKeyStore.password)
    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(keystore, BogusKeyStore.password)

    val context = SSLContext.getInstance("TLS")
    context.init(kmf.getKeyManagers, null, null)
    val addr = new java.net.InetSocketAddress(java.net.InetAddress.getByName("0.0.0.0"), 8765)

    Try {
      val srv = new ServerBootstrap
      srv.group(eventLoop1.get, eventLoop2.get)
        .localAddress(addr)
        .channel(classOf[NioServerSocketChannel])
        .childOption[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
        .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
        .childOption[java.lang.Boolean](ChannelOption.SO_REUSEADDR, true)
        .childOption[java.lang.Integer](ChannelOption.SO_LINGER, 0)
        .childHandler(new ChannelInitializer[SocketChannel] {
          override def initChannel(ch: SocketChannel) {
            val pipeline = ch.pipeline()

            // add SSL
            val engine = context.createSSLEngine()
            engine.setUseClientMode(false)
            NextProtoNego.put(engine, new NpnServerProvider)
            NextProtoNego.debug = true

            pipeline.addLast("ssl", new SslHandler(engine))
            pipeline.addLast("chooser", new SpdyOrHttpHandler)
          }
        })
      srv.bind().syncUninterruptibly()
      println("Listening on " + addr.getPort)
      srv
    } match {
      case Success(v) =>
      case Failure(f) => println("Unable to bind to " + addr.getPort); stop()
    }

    //info("Ready")

    // Add Shutdown Hook to cleanly shutdown Netty
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run() { PS.stop() }
    })
  }

  def stop() {
    //info("Shutting down")

    // Shut down all event loops to terminate all threads.
    eventLoop1.map(_.shutdownGracefully())
    eventLoop1 = None

    eventLoop2.map(_.shutdownGracefully())
    eventLoop2 = None

    //info("Shutdown complete")
  }
}
