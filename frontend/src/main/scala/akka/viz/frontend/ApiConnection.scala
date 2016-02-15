package akka.viz.frontend

import org.scalajs.dom
import org.scalajs.dom.raw._

import scala.concurrent.Promise
import scala.concurrent._
import scala.util.Success

trait Upstream {
  def send(msg: String): Unit
}

object ApiConnection {


  def apply(url: String, fn: MessageEvent => Unit)(implicit ec: ExecutionContext): Future[Upstream] = {

    def createWebsocket: WebSocket = {
      val ws = new WebSocket(url)
      ws
    }

    val wsPromise = Promise[WebSocket]()
    val newWs = createWebsocket
    newWs.onopen = { e: Event =>
      wsPromise.success(newWs)
    }
    newWs.onerror = { e: ErrorEvent =>
      wsPromise.failure(new Exception(e.message))
    }

    val wsFuture: Future[WebSocket] = wsPromise.future

    wsFuture
      .andThen {
        case Success(ws) =>
          ws.onclose = { ce: CloseEvent => dom.console.log(ce) }
          ws.onerror = { ee: ErrorEvent => dom.console.log(ee) }
          ws.onmessage = fn
      }.map(ws =>
        new Upstream {
          override def send(msg: String): Unit = ws.send(msg)
      })


  }

}