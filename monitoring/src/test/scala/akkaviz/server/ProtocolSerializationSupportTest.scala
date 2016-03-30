package akkaviz.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.BinaryMessage
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import akkaviz.protocol.{SetEnabled, IO, Killed}
import org.scalatest.{Matchers, FunSuite}
import org.scalatest.concurrent.ScalaFutures

class ProtocolSerializationSupportTest extends FunSuite with ScalaFutures with Matchers {

  private[this] implicit val system = ActorSystem()
  private[this] implicit val materializer = ActorMaterializer()

  import ProtocolSerializationSupport._

  test("websocketMessageToClientMessage") {
    val msg = SetEnabled(true)
    val wsMessage = BinaryMessage(ByteString(IO.write(msg)))
    val res = Source.single(wsMessage).via(websocketMessageToClientMessage).runWith(Sink.head)
    whenReady(res) {
      _ shouldBe msg
    }
  }

  test("protocolServerMessageToByteString") {
    val msg = Killed("ref")
    val res = Source.single(msg).via(protocolServerMessageToByteString).runWith(Sink.head)
    whenReady(res) {
      serialized =>
        IO.readServer(serialized.asByteBuffer) shouldBe msg
    }
  }

}
