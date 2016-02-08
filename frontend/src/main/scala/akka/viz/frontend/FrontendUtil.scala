package akka.viz.frontend

import org.scalajs.dom.html.Element
import org.scalajs.dom.window

import scalatags.JsDom.all._

object FrontendUtil {

  def webSocketUrl(path: String) = {
    val l = window.location
    (if (l.protocol == "https:") "wss://" else "ws://") +
      l.hostname +
      (if ((l.port != 80) && (l.port != 443)) ":" + l.port else "") +
      l.pathname + path
  }

  def actorComponent(actorRef: String): Element = {
    def isUser(ref: String): Boolean = ref.contains("user")
    val shortActorName = actorRef.split('/').drop(3).mkString("/")
    span(
      "data-toggle".attr := "tooltip", "data-placement".attr := "top", title := actorRef, shortActorName
    ).render
  }

}
