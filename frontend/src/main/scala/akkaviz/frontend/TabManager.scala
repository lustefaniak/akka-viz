package akkaviz.frontend

import akkaviz.frontend.components.ActorStateTab
import org.scalajs.dom._
import rx.Ctx

class TabManager(repo: ActorRepository, upstreamConnection: ApiConnection.Upstream)(implicit ctx: Ctx.Owner) {

  def openActorDetails(actorRef: String): Unit = {
    //FIXME: prevent opening same actor tab multiple times, focus instead
    val stateVar = repo.state(actorRef)
    val tab: ActorStateTab = new ActorStateTab(stateVar, upstreamConnection.send)
    tab.attach(document.querySelector("#right-pane"))
  }
}
