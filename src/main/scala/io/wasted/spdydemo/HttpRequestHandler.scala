package io.wasted.spdydemo

import io.netty.channel.{ ChannelFutureListener, SimpleChannelInboundHandler, ChannelHandlerContext}
import io.netty.handler.codec.http.{FullHttpRequest, HttpHeaders}
import io.netty.handler.codec.http.HttpResponseStatus._

class HttpRequestHandler extends SimpleChannelInboundHandler[FullHttpRequest]() {

  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
    val keepAlive = HttpHeaders.isKeepAlive(msg)
    val body = "Served via HTTP. This means that your browser does not support SPDY."
    val contenType = "text/html; charset=UTF-8"
    val response = OurHttpResponse(msg.getProtocolVersion, OK, Some(body), Some(contenType), keepAlive)
    val future = ctx.channel().writeAndFlush(response)
    if (!keepAlive) future.addListener(ChannelFutureListener.CLOSE)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    //ExceptionHandler.apply(ctx, cause).map(_.printStackTrace)
    //if (ctx.channel().isOpen) ctx.close()
  }
}
