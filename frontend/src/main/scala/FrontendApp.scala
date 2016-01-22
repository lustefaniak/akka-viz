import scala.scalajs.js
import scala.scalajs.js._
import org.scalajs.dom._


@js.native
object DOMGlobalScope extends js.GlobalScope {
  val graph: js.Dynamic = js.native
}

object FrontendApp extends JSApp {

  def webSocketUrl(path: String) = {
    val l = window.location
    (if (l.protocol == "https:") "wss://" else "ws://") +
      l.hostname +
      (if ((l.port != 80) && (l.port != 443)) ":" + l.port else "") +
      l.pathname + path
  }

  def actorName(actorRef: String): String = {
    actorRef.split("/").drop(3).mkString("/").split("#").head
  }

  trait Message extends js.Any {
    def sender: String

    def receiver: String

    def message: Any
  }

  val createdLinks = scala.collection.mutable.Set[String]()
  val graph = DOMGlobalScope.graph

  def main(): Unit = {

    val webSocket = new WebSocket(webSocketUrl("stream"))
    webSocket.onmessage = (messageEvent: MessageEvent) => {
      val message = JSON.parse(messageEvent.data.asInstanceOf[String]).asInstanceOf[Message]

      val sender = actorName(message.sender)
      val recevier = actorName(message.receiver)

      val linkId = s"${sender}->${recevier}"
      if (!createdLinks(linkId)) {
        createdLinks.add(linkId)
        graph.beginUpdate()
        graph.addLink(sender, recevier, linkId)
        graph.endUpdate()
      }
    }

  }
}