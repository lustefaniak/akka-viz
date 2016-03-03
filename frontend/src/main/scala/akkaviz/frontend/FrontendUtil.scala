package akkaviz.frontend

import org.scalajs.dom.{Element, window}

import scala.annotation.tailrec
import scala.scalajs.js
import scalatags.JsDom.all._

object FrontendUtil {

  def webSocketUrl(path: String) = {
    val l = window.location
    (if (l.protocol == "https:") "wss://" else "ws://") +
      l.hostname +
      (if ((l.port != 80) && (l.port != 443)) ":" + l.port else "") +
      l.pathname + path
  }

  @tailrec
  def findParentWithAttribute(elem: Element, attributeName: String): js.UndefOr[Element] = {
    if (elem == null || elem.hasAttribute(attributeName))
      elem
    else
      findParentWithAttribute(elem.parentNode.asInstanceOf[Element], attributeName)
  }

  @inline
  def actorComponent(actorRef: String): Element = {
    def isUser(ref: String): Boolean = ref.contains("user")
    span(
      "data-toggle".attr := "tooltip", "data-placement".attr := "top", title := actorRef, shortActorName(actorRef)
    ).render
  }

  @inline
  def shortActorName(actorRef: String) = actorRef.split('/').drop(3).mkString("/")

  @inline
  def systemName(actorRef: String): String = actorRef.split('/')(2)

}
