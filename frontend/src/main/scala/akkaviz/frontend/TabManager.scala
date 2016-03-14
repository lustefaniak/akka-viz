package akkaviz.frontend

import akkaviz.frontend.components.{ClosableTab, Tab, ActorStateTab}
import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLElement
import rx.Ctx

import scala.scalajs.js

class TabManager(repo: ActorRepository, upstreamConnection: ApiConnection.Upstream)(implicit ctx: Ctx.Owner) {

  val tabs: js.Dictionary[Tab] = js.Dictionary.empty

  def openActorDetails(actorRef: String): Unit = {
    activate(tabs.getOrElseUpdate(ActorStateTab.stateTabId(actorRef), {
      val stateVar = repo.state(actorRef)
      val tab: ActorStateTab = new ActorStateTab(stateVar, upstreamConnection.send)
      tab.attach(document.querySelector("#right-pane"))
      tab.tab.querySelector("a.close-tab").onClick({() => close(tab)})
      tab.tab.querySelector("a[data-toggle]").addEventListener("click", handleMiddleClick(tab) _)
      tab
    }))
  }

  def activate(tab: Tab): Unit = {
    document.querySelector(s"""a[href*="${tab.tabId}"]""").asInstanceOf[HTMLElement].click()
  }

  def close(target: ClosableTab): Unit = {
    if(target.isActive) activateSiblingOf(target)
    target.tab.parentNode.removeChild(target.tab)
    target.tabBody.parentNode.removeChild(target.tabBody)
    target.onClose()
    tabs.delete(target.tabId)
  }

  private def activateSiblingOf(ct: ClosableTab): Unit = {
    Option(ct.tab.nextElementSibling).orElse(Option(ct.tab.previousElementSibling)).map { s =>
      s.querySelector("a[data-toggle]").asInstanceOf[HTMLElement]
    }.foreach{ _.click() }

  }

  private def handleMiddleClick(tab: ClosableTab)(e: MouseEvent): Unit = {
    if(e.button == 1) {
      e.preventDefault()
      close(tab)
    }
  }
}
