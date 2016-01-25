package akka.viz.frontend

import akka.viz.protocol._
import org.querki.jquery.{JQueryStatic => jQ}
import org.scalajs.dom.{onclick => oc, _}
import rx._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.{Dictionary, JSApp, JSON}
import scalatags.JsDom.all._

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
        val receiver = actorName(rcv.receiver)
        addActorsToSeen(sender, receiver)

        val linkId = s"${sender}->${receiver}"
        if (!createdLinks(linkId)) {
          createdLinks.add(linkId)
          graph.beginUpdate()
          graph.addLink(sender, receiver, linkId)
          graph.endUpdate()
        }

      case ac: AvailableClasses =>
        seenMessages() = ac.availableClasses.toSet
    }

  }

  val seenActors = Var[Set[String]](Set())
  val selectedActor = Var("")
  val seenMessages = Var[Set[String]](Set())
  val selectedMessages = Var[Set[String]](Set())

  private def addActorsToSeen(actorName: String*): Unit = {
    val previouslySeen = seenActors.now
    val newSeen = previouslySeen ++ actorName.filterNot(previouslySeen(_))
    seenActors() = newSeen
  }

  @JSExport("pickActor")
  def pickActor(actorPath: String): Unit = {
    if (selectedActor.now == actorPath) {
      console.log(s"Unselected '$actorPath' actor")
      selectedActor() = ""
    } else {
      console.log(s"Selected '$actorPath' actor")
      selectedActor() = actorPath
    }
  }

  def main(): Unit = {
    val upstream = ApiConnection(webSocketUrl("stream"), handleDownstream)

    val actorsObs = Rx.unsafe {
      (seenActors(), selectedActor())
    }.triggerLater {
      val seen = seenActors.now.toList.sorted
      val selected = selectedActor.now

      val content = div(`class` := "collection", ul(seen.map {
        actorName =>
          li(`class` := "collection-item", if (selected == actorName) b(actorName) else actorName, onclick := {
            () => pickActor(actorName)
          })
      }))

      val actorTree = document.getElementById("actortree")
      actorTree.innerHTML = ""
      actorTree.appendChild(content.render)
    }

    val messagesObs = Rx.unsafe {
      (seenMessages(), selectedMessages())
    }.triggerLater {

      val seen = seenMessages.now.toList.sorted
      val selected = selectedMessages.now

      val content = div(`class` := "collection", ul(seen.map {
        clazz =>
          val contains = selected(clazz)
          li(`class` := "collection-item",
            if (contains) input(`type` := "checkbox", checked := true) else input(`type` := "checkbox"),
            if (contains) b(clazz) else clazz,
            onclick := {
              () =>
                console.log(s"Toggling ${clazz} now it will be ${!contains}")
                selectedMessages() = if (contains) selected - clazz else selected + clazz
            })
      }))

      val messages = document.getElementById("messages")
      messages.innerHTML = ""
      messages.appendChild(content.render)

      console.log(s"Will send allowedClasses: ${selected.mkString("[", ",", "]")}")
      upstream.send(JSON.stringify(Dictionary("allowedClasses" -> js.Array(selected.toSeq: _*))))
    }

  }
}