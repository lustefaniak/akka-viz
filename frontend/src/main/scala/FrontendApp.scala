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

  trait Received extends js.Any {
    def sender: String

    def receiver: String

    def message: Any
  }

  trait AvailableClasses extends js.Any {
    def availableClasses: Array[String]
  }

  val createdLinks = scala.collection.mutable.Set[String]()
  val graph = DOMGlobalScope.graph

  def main(): Unit = {

    import org.querki.jquery.{JQueryStatic => jQ}

    val webSocket = new WebSocket(webSocketUrl("stream"))
    webSocket.onmessage = (messageEvent: MessageEvent) => {
      val message: Dynamic = JSON.parse(messageEvent.data.asInstanceOf[String])
      // todo figure out pickling instead of checking if fields are defined
      message match {
        case _ if !isUndefined(message.sender) && !isUndefined(message.receiver) =>
          val rcv = message.asInstanceOf[Received]

          val sender = actorName(rcv.sender)
          val recevier = actorName(rcv.receiver)

          val linkId = s"${sender}->${recevier}"
          if (!createdLinks(linkId)) {
            createdLinks.add(linkId)
            graph.beginUpdate()
            graph.addLink(sender, recevier, linkId)
            graph.endUpdate()
          }
        case _ if !isUndefined(message.availableClasses) =>
          val ac = message.asInstanceOf[AvailableClasses]

          ac.availableClasses
            .foreach{ clsName =>
              val elem = jQ(s"""<input type="checkbox" value="$clsName" id="$clsName" /><label for="$clsName">$clsName</label><br>""")
              if (jQ(s"form input").filter((e: Element) => e.id == clsName).length == 0) { jQ("form").append(elem) }
            }

      }
    }

    jQ("form").click({e: Element =>
      val checked = jQ("form :checked").mapElems(elem => jQ(elem).valueString)
      webSocket.send(JSON.stringify(Dictionary("allowedClasses" -> js.Array(checked :_*))))
    })
  }
}