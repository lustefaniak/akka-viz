package akkaviz.server

import java.io.{InputStream, OutputStream}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.TestSubscriber.OnNext
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.util.ByteString
import ammonite.ops.Path
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration._

class WebSocketReplTest extends FunSuite with ScalaFutures with Matchers {

  private[this] implicit val system = ActorSystem()
  private[this] implicit val materializer = ActorMaterializer()

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private[this] def createRepl(fn: Function2[InputStream, OutputStream, Unit]) = new WebSocketRepl {
    override protected def startReplProcessing(
      in: InputStream, out: OutputStream, err: OutputStream, homePath: Path
    ): Unit = {
      fn(in, out)
    }
  }

  private[this] def wrongMsg = fail("Response should be BinaryMessage.Strict")

  private[this] val pingPongAndClose: Function2[InputStream, OutputStream, Unit] = {
    (in, out) =>
      val ab = new Array[Byte](4)
      in.read(ab)
      ByteString(ab).utf8String shouldBe "ping"
      out.write("pong".getBytes)
      in.close()
      out.flush()
      out.close()
  }

  private[this] def expectString(string: String)(chunks: Seq[Message]): Unit = {
    chunks.collect {
      case BinaryMessage.Strict(data) => data.utf8String
      case _                          => wrongMsg
    }.mkString should include(string)
  }

  test("It can work in both ways") {
    val wsRepl = createRepl(pingPongAndClose)
    val res = Source.single(BinaryMessage.Strict(ByteString("ping"))).via(wsRepl.replWebsocketFlow).runWith(Sink.seq)
    whenReady(res)(expectString("pong"))
  }

  test("It forwards exceptions from the repl") {
    val wsRepl = createRepl {
      (in, out) =>
        throw new Exception("Failure in REPL")
    }
    val res = Source.single(BinaryMessage.Strict(ByteString("ping"))).via(wsRepl.replWebsocketFlow).runWith(Sink.seq)
    whenReady(res)(expectString("Failure in REPL"))
  }

  test("Repl accepts TextMessage too") {
    val wsRepl = createRepl(pingPongAndClose)
    val res = Source.single(TextMessage.Strict("ping")).via(wsRepl.replWebsocketFlow).runWith(Sink.seq)
    whenReady(res)(expectString("pong"))
  }

  test("It sends Keep Alive") {
    val wsRepl = new WebSocketRepl {
      override protected def startReplProcessing(
        in: InputStream, out: OutputStream, err: OutputStream, homePath: Path
      ): Unit = {
        pingPongAndClose(in, out)
      }

      override protected def replKeepAliveEvery: FiniteDuration = 10.millis

      override val replKeepAliveMessage: Message = super.replKeepAliveMessage
    }

    val keepAliveMessage = wsRepl.replKeepAliveMessage

    val (pub, sub) = TestSource.probe[Message]
      .via(wsRepl.replWebsocketFlow)
      .toMat(TestSink.probe[Message])(Keep.both)
      .run()

    sub.ensureSubscription()
    sub.request(1)
    sub.requestNext() shouldBe keepAliveMessage
    pub.sendComplete()
  }

  test("It closes on 'exit' ") {

    val keepAliveMessage = BinaryMessage.Strict(ByteString("keep-alive"))

    val wsRepl = new WebSocketRepl {}

    val (pub, sub) = TestSource.probe[Message]
      .via(wsRepl.replWebsocketFlow)
      .toMat(TestSink.probe[Message])(Keep.both)
      .run()

    sub.ensureSubscription()
    sub.request(1000)
    //wait for prompt
    sub.receiveWhile(5.seconds) {
      case OnNext(BinaryMessage.Strict(data)) if !data.utf8String.contains("@") => data.utf8String
    }

    //send exit
    pub.sendNext(TextMessage.Strict("exit\n"))

    //wait for termination
    sub.receiveWhile(5.seconds) {
      case OnNext(BinaryMessage.Strict(data)) if !data.utf8String.contains("Bye!") => data.utf8String
    }
    sub.expectNext() match {
      case BinaryMessage.Strict(data) =>
        data.utf8String should include("Bye!")
      case _ => wrongMsg
    }

    sub.expectComplete()
    pub.sendComplete()
  }

}
