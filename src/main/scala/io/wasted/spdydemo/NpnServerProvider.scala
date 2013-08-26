package io.wasted.spdydemo

import org.eclipse.jetty.npn.NextProtoNego.ServerProvider
import scala.collection.JavaConverters._

class NpnServerProvider extends ServerProvider {
  private final val default: String = "http/1.1"
  private final val supported = List("spdy/2", "spdy/3", "http/1.1").asJava

  private var protocol: String = _
  def getSelectedProtocol = protocol

  override def protocolSelected(proto: String) {
    protocol = proto
  }

  override def unsupported() {
    protocol = default
  }

  override def protocols() = supported
}
