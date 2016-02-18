package akkaviz.frontend

import akkaviz.protocol

import scala.scalajs.js

trait MailboxDisplay {
  private val graph = DOMGlobalScope.graph

  def handleMailboxStatus(mb: protocol.MailboxStatus): Unit = {
    // todo: add a actor metadata type when we'll display more info on the graph?
    graph.addNode(mb.owner, js.Dictionary("mailboxSize" -> mb.size))
  }
}
