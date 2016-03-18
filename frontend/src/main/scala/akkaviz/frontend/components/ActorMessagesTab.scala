package akkaviz.frontend.components

import akkaviz.frontend.{ActorLink, ActorPath, FrontendUtil, Router}
import org.scalajs.dom.{Element => domElement}

class ActorMessagesTab(actorRef: ActorPath) extends ArchiveTab {

  import ActorMessagesTab._

  val tabId = stateTabId(actorRef)
  val name = s"${FrontendUtil.shortActorName(actorRef)} Messages"

  val loadUrl = Router.messagesOf(actorRef)
}

object ActorMessagesTab {
  def stateTabId(actorRef: ActorPath): String = {
    s"actor-messages-${actorRef.replaceAll("[\\/|\\.|\\\\|\\$]", "-").filterNot(_ == ':')}"
  }
}

