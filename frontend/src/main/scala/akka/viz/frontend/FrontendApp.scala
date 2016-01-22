package akka.viz.frontend

import akka.viz.protocol._
import org.querki.jquery.{JQueryStatic => jQ}
import org.scalajs.dom._

import scala.scalajs.js
import scala.scalajs.js._


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

  val createdLinks = scala.collection.mutable.Set[String]()
  val graph = DOMGlobalScope.graph

  private def handleDownstream(messageEvent: MessageEvent): Unit = {
    val message: ApiServerMessage = ApiMessages.read(messageEvent.data.asInstanceOf[String])

    message match {
      case rcv: Received =>

        val sender = actorName(rcv.sender)
        val recevier = actorName(rcv.receiver)

        val linkId = s"${sender}->${recevier}"
        if (!createdLinks(linkId)) {
          createdLinks.add(linkId)
          graph.beginUpdate()
          graph.addLink(sender, recevier, linkId)
          graph.endUpdate()
        }

      case ac: AvailableClasses =>

        ac.availableClasses
          .foreach { clsName =>
            val elem = jQ(s"""<input type="checkbox" value="$clsName" id="$clsName" /><label for="$clsName">$clsName</label><br>""")
            if (jQ(s"form input").filter((e: Element) => e.id == clsName).length == 0) {
              jQ("form").append(elem)
            }
          }
    }

  }

  def main(): Unit = {
    val upstream = ApiConnection(webSocketUrl("stream"), handleDownstream)

    jQ("form").click({ e: Element =>
      val checked = jQ("form :checked").mapElems(elem => jQ(elem).valueString)
      upstream.send(JSON.stringify(Dictionary("allowedClasses" -> js.Array(checked: _*))))
    })
  }
}