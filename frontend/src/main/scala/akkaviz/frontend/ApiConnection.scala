package akkaviz.frontend

import org.scalajs.dom
import org.scalajs.dom.raw._

import scala.concurrent.{Promise, _}
import scala.scalajs.js.JavaScriptException

trait Upstream {
  def send(msg: String): Unit
}

object ApiConnection {

  def apply(url: String, fn: MessageEvent => Unit, maxRetries: Int = 1)(implicit ec: ExecutionContext): Future[WebSocket] = {

    def createWebsocket: WebSocket = {
      val ws = new WebSocket(url)
      ws
    }

    val wsPromise = Promise[WebSocket]()
    val newWs = createWebsocket
    newWs.onclose = { ce: CloseEvent =>
      dom.setTimeout(() => wsPromise.failure(new JavaScriptException()), 2000)
    }
    newWs.onopen = { e: Event =>
      dom.console.log("API websocket connection established")
      newWs.onmessage = fn
      wsPromise.success(newWs)
    }

    val wsFuture: Future[WebSocket] = wsPromise.future

    wsFuture
      .recoverWith {
        case e: JavaScriptException if maxRetries > 0 =>
          dom.console.log(s"failed to establish connection to $url, retrying $maxRetries more times")
          apply(url, fn, maxRetries - 1)
      }

  }

}