package akkaviz.frontend

import scala.scalajs.js.URIUtils.encodeURIComponent

object Router {
  def messagesBetween(from: ActorPath, to: ActorPath): String = {
    s"/messages/between/${encodeURIComponent(from)}/${encodeURIComponent(to)}"
  }
}
