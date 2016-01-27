package akka.viz.frontend

import akka.viz.protocol
import scalajs.js

trait MailboxDisplay extends FrontendUtil {
  private val graph = DOMGlobalScope.graph

  def handleMailboxStatus(mb: protocol.MailboxStatus): Unit = {
    // todo: add a actor metadata type when we'll display more info on the graph?
    graph.addNode(actorName(mb.owner), js.Dictionary.apply("mailboxSize" -> mb.size))
  }
}
