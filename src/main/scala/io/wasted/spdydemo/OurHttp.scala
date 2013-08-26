package io.wasted.spdydemo

import io.netty.buffer._
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.HttpHeaders._
import io.netty.handler.codec.http.HttpHeaders.Names._
import io.netty.handler.codec.http.HttpVersion._

import scala.collection.JavaConverters._

object OurHttpResponse {
  lazy val serverToken = Some("wasted.io spdy")

  def apply(
    version: HttpVersion,
    status: HttpResponseStatus,
    body: Option[String] = None,
    mime: Option[String] = None,
    close: Boolean = true,
    headers: Map[String, String] = Map()): FullHttpResponse = {
    val res = body match {
      case Some(body) =>
        val content = Unpooled.wrappedBuffer(body.getBytes("UTF-8"))
        val res = new DefaultFullHttpResponse(version, status, content)
        setContentLength(res, content.readableBytes())
        res
      case None =>
        val res = new DefaultFullHttpResponse(HTTP_1_1, status)
        setContentLength(res, 0)
        res
    }

    mime match {
      case Some(contenttype) => res.headers.set(CONTENT_TYPE, contenttype)
      case _ =>
    }

    serverToken.foreach { t => res.headers.set(SERVER, t) }
    headers.foreach { h => res.headers.set(h._1, h._2) }

    if (close) res.headers.set(CONNECTION, Values.CLOSE)
    res
  }
}

object OurHttpHeaders {
  trait Headers {
    def get(key: String): Option[String] = getAll(key).headOption
    def apply(key: String): String = get(key).getOrElse(scala.sys.error("Header doesn't exist"))
    def getAll(key: String): Iterable[String]
    val length: Int
  }

  def get(request: HttpRequest): Headers = {
    val headers: Map[String, Seq[String]] = request.headers.names.asScala.map(key =>
      key.toLowerCase -> Seq(request.headers.get(key))).toMap

    new Headers {
      def getAll(key: String): Iterable[String] = headers.get(key.toLowerCase) getOrElse Seq()
      override def toString = headers.toString()
      override lazy val length = headers.size
    }
  }
}

