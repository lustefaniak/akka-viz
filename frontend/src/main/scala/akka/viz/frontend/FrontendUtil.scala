package akka.viz.frontend

import org.scalajs.dom._

trait FrontendUtil {

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

}
