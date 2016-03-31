package akkaviz.server

import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import akkaviz.protocol

trait ProtocolSerializationSupport {

  def protocolServerMessageToByteString: Flow[protocol.ApiServerMessage, ByteString, Any] = Flow[protocol.ApiServerMessage].map {
    msg => ByteString(protocol.IO.write(msg))
  }

  def websocketMessageToClientMessage: Flow[Message, protocol.ApiClientMessage, _] = Flow[Message].collect {
    case BinaryMessage.Strict(msg) =>
      protocol.IO.readClient(msg.asByteBuffer)
  }

}

object ProtocolSerializationSupport extends ProtocolSerializationSupport