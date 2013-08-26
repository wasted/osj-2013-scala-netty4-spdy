package io.wasted.spdydemo

import io.netty.channel.{ChannelFuture, ChannelFutureListener, ChannelHandlerContext}
import io.netty.handler.codec.http.{HttpHeaders, FullHttpRequest}
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.spdy._
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil


class SpdyRequestHandler extends HttpRequestHandler {
  final val body = {
    <html>
      <head>
        <title>wasted.io SPDY Demo</title>
        <script src="/site.js" type="text/javascript"></script>
        <link href="/style.css" rel="stylesheet"/>
      </head>
      <body>
        SPDY works!
      </body>
    </html>
  }

  /**
   * SPDY Channel ID the Server uses to push to the client (positive even integer, 2 in our example)
   */
  var ourSpdyId = 2

  /**
   * This is an example for a more granular approach for pushing data
   * @param ctx Netty ChannelHandelerContext
   * @param msg The initial HTTP request
   * @param currentStreamId SPDY Channel ID the Client used to initiate the SPDY request (positive uneven integer)
   * @param name Resource name (e.g. foo.xml)
   * @param message Body to be delivered
   * @param contentType Content-Type to be transmitted in Headers
   * @param fin Finish this stream
   */
  def pushContent(ctx: ChannelHandlerContext, msg: FullHttpRequest, currentStreamId: Int,
                  name: String, message: String, contentType: String, fin: Boolean) {
    // this is not really safe and should not be used in production ;)
    val url = s"https://${msg.headers().get("host")}/$name"
    println(s"Pushing resource: $url")

    // Allocate the buffer for our message
    val buf = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8)
    // Create a SPDY Data frame
    val content = new DefaultSpdyDataFrame(currentStreamId, buf)

    content.setLast(fin)

    // Create headers for the Stream-Id the Server is using and reference the stream-ID the client uses.
    //val headers = new DefaultSpdyHeadersFrame(ourSpdyId)
    val headers = new DefaultSpdySynStreamFrame(ourSpdyId, currentStreamId, 0)
    headers.setUnidirectional(true)
    SpdyHeaders.setStatus(2, headers, OK)
    SpdyHeaders.setVersion(2, headers, msg.getProtocolVersion)
    SpdyHeaders.setUrl(2, headers, url)
    headers.headers().add("Content-Type", contentType)
    headers.headers().add("Content-Length", buf.readableBytes())
    headers.setLast(false)

    ctx.channel().write(headers)
    ctx.channel().write(content)
    println(headers.toString)
    println(content.toString)
    println("----")
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
    val currentStreamId = Option(SpdyHttpHeaders.getStreamId(msg)) getOrElse 0

    // Deny everything gracefully in order to not close the SPDY connect
    // Normally we should deliver those resources by HTTP standards when the client requests them
    // This is the real proof that it does not request the resources by traditional http means
    // (or at least fails it it tries to)
    if (!msg.getUri.matches("/")) {
      println(s"404'ing a request to ${msg.getUri}")
      val headers = new DefaultSpdySynStreamFrame(currentStreamId, 0, 0)
      SpdyHeaders.setStatus(2, headers, NOT_FOUND)
      SpdyHeaders.setVersion(2, headers, msg.getProtocolVersion)
      SpdyHeaders.setUrl(2, headers, msg.getUri)
      ctx.channel().writeAndFlush(headers)
      // we could also do
      //ctx.channel().writeAndFlush(OurHttpResponse(msg.getProtocolVersion, NOT_FOUND, close = false))
      return
    }

    println(msg)
    println("---")

    // Sending a 100 continue if needed
    if (HttpHeaders.is100ContinueExpected(msg)) {
      ctx.channel().writeAndFlush(OurHttpResponse(msg.getProtocolVersion, CONTINUE, close = false))
    }

    // Set some SPDY settings. 4 is the number of maximum streams through this SPDY connection.
    // Other settings would be up- or downstream in kbits (which can be nice since we're going to push resources)
    val settings = new DefaultSpdySettingsFrame
    settings.setValue(4, 300)
    ctx.channel().writeAndFlush(settings)

    // Push our site.js and style.css
    pushContent(ctx, msg, currentStreamId, "site.js", "alert('via SPDY!');", "text/javascript; charset=utf-8", false)
    pushContent(ctx, msg, currentStreamId, "style.css", "body { background-color: gray; }", "text/css", false)

    // Reply on the request (client initiated) stream with the HTML
    // which tells the client to consume pushed resources
    val headers = Map(
      SpdyHttpHeaders.Names.STREAM_ID -> currentStreamId.toString,
      SpdyHttpHeaders.Names.PRIORITY -> "0")

    val contenType = "text/html; charset=UTF-8"
    val response = OurHttpResponse(msg.getProtocolVersion, OK, Some(body.toString()), Some(contenType), false, headers)

    println(response)
    println("---")

    // Write the response to the client
    ctx.channel().write(response)

    ctx.channel().flush()

    // Add a Close-Listener to print a line once the SPDY connection closes
    ctx.channel.closeFuture().addListener(new ChannelFutureListener() {
      override def operationComplete(cf: ChannelFuture) {
        println("SPDY connection closed")
      }
    })
  }
}