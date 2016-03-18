package akkaviz.frontend.components

import akkaviz.frontend.{ActorLink, FrontendUtil, Router}
import org.scalajs.dom.{Element => domElement}

class LinkStateTab(link: ActorLink) extends ArchiveTab {
  import LinkStateTab._

  val tabId = stateTabId(link)
  val name = s"${FrontendUtil.shortActorName(link.from)} → ${FrontendUtil.shortActorName(link.to)}"

  val loadUrl = Router.messagesBetween(link.from, link.to)
}

object LinkStateTab {
  def stateTabId(link: ActorLink): String = {
    s"link-state-${(link.from + link.to).replaceAll("[\\/|\\.|\\\\|\\$]", "-").filterNot(_ == ':')}"
  }
}

