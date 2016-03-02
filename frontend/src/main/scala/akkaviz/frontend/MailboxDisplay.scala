package akkaviz.frontend

import akkaviz.frontend.components.GraphView
import akkaviz.protocol

import scala.scalajs.js

trait MailboxDisplay {
  def handleMailboxStatus(mb: protocol.MailboxStatus, graphView: GraphView): Unit = {
    //graphView.ensureNodeExists(mb.owner, FrontendUtil.shortActorName(mb.owner))
  }
}
