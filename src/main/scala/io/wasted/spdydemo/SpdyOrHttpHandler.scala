package io.wasted.spdydemo

import io.netty.handler.codec.spdy.SpdyOrHttpChooser
import io.netty.handler.codec.spdy.SpdyOrHttpChooser.SelectedProtocol
import org.eclipse.jetty.npn.NextProtoNego
import io.netty.channel.ChannelHandlerContext
import io.wasted.util.http.ExceptionHandler
import javax.net.ssl.SSLEngine

class SpdyOrHttpHandler extends SpdyOrHttpChooser(1024 * 1024, 1024 * 1024) {
  override protected def getProtocol(engine: SSLEngine): SelectedProtocol = {
    val provider = NextProtoNego.get(engine).asInstanceOf[NpnServerProvider]
    val protocol = Option[String](provider.getSelectedProtocol)
    println(s"NPN Provider: ${provider.toString}: $protocol")
    protocol match {
      case Some("spdy/2") => SelectedProtocol.SPDY_2
      case Some("spdy/3") => SelectedProtocol.SPDY_3
      case Some("http/1.0") => SelectedProtocol.HTTP_1_0
      case Some("http/1.1") => SelectedProtocol.HTTP_1_1
      case _ => SelectedProtocol.UNKNOWN
    }
  }

  override protected def createHttpRequestHandlerForHttp() = new HttpRequestHandler
  override protected def createHttpRequestHandlerForSpdy() = new SpdyRequestHandler

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    ExceptionHandler.apply(ctx, cause).map(_.printStackTrace)
    if (ctx.channel().isOpen) ctx.close()
  }
}