package akka.viz.frontend


import org.scalajs.dom._
import org.scalajs.dom.raw.WebSocket

object ApiConnection {

  trait Upstream {
    def send(msg: String): Unit
  }

  def apply(url: String, fn: MessageEvent => Unit): Upstream = {

    var webSocket: Option[WebSocket] = None

    //FIXME: implement reconnect and wait for `onopen` event before sending
    def createWebsocket: WebSocket = {
      val ws = new WebSocket(url)
      ws.onmessage = (messageEvent: MessageEvent) => {
        fn(messageEvent)
      }
      ws.onclose = (c: CloseEvent) => {
        console.log(c)
        webSocket = None
      }
      ws.onerror = (e: ErrorEvent) => {
        console.log(e)
        webSocket = None
      }
      webSocket = Some(ws)
      ws
    }
    webSocket = Some(createWebsocket)

    def getWs = webSocket.getOrElse(createWebsocket)

    new Upstream {
      //FIXME: need to wait for `onopen` before sending
      override def send(msg: String): Unit = getWs.send(msg)
    }

  }

}
