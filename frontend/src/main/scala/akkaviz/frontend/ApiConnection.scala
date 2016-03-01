package akkaviz.frontend

import akkaviz.protocol.{ApiClientMessage, IO}
import org.scalajs.dom
import org.scalajs.dom.raw._

import scala.concurrent.duration._
import scala.concurrent.{Promise, _}
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBufferOps
import scala.scalajs.js.{JavaScriptException, timers}

trait Upstream {
  def send(msg: String): Unit
}

object ApiConnection {

  case class ApiUpstream(private val ws: WebSocket) extends AnyVal {
    def send(msg: ApiClientMessage): Unit = {
      val encoded = IO.write(msg)
      ws.send(new TypedArrayBufferOps(encoded).arrayBuffer())
    }

    def onclose = ws.onclose
    def onclose_=(fn: js.Function1[CloseEvent, _]): Unit = {
      ws.onclose = fn
    }

    def onerror = ws.onerror
    def onerror_=(fn: js.Function1[ErrorEvent, _]): Unit = {
      ws.onerror = fn
    }

  }

  def apply(url: String, onWsOpen: ApiUpstream => Unit, fn: MessageEvent => Unit, maxRetries: Int = 1)(implicit ec: ExecutionContext): Future[ApiUpstream] = {

    def createWebsocket: WebSocket = {
      val ws = new WebSocket(url)
      ws.binaryType = "arraybuffer"
      ws
    }

    val wsPromise = Promise[ApiUpstream]()
    val newWs = createWebsocket
    newWs.onclose = { ce: CloseEvent =>
      timers.setTimeout(2.seconds) {
        wsPromise.failure(new JavaScriptException())
      }
    }
    newWs.onopen = { e: Event =>
      dom.console.log("API websocket connection established")
      newWs.onmessage = fn
      val wrapped = ApiUpstream(newWs)
      onWsOpen(wrapped)
      wsPromise.success(wrapped)
    }

    val wsFuture: Future[ApiUpstream] = wsPromise.future

    wsFuture
      .recoverWith {
        case e: JavaScriptException if maxRetries > 0 =>
          dom.console.log(s"failed to establish connection to $url, retrying $maxRetries more times")
          apply(url, onWsOpen, fn, maxRetries - 1)
      }

  }

}