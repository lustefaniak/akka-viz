package akkaviz.frontend.tabs

import akkaviz.frontend.components.{ClosableTab, Component, Tab}
import org.scalajs.dom.Element
import org.scalajs.dom.raw.HTMLAnchorElement

import scalatags.JsDom.all._

class TabMenu(elemId: String, tabs: Tab*) extends Component {

  private[this] val navbar = ul(cls := "nav nav-tabs")
  private[this] val tabContent = div(cls := "tab-content")
  private[this] val rendered: Element = div(id := elemId, cls := "menu tab-container", navbar, tabContent).render

  private[this] def attachTab(tab: Tab): Unit = {
    tab.attach(rendered)
    tab.onCreate()

    tab match {
      case ct: ClosableTab =>
      case _ =>
    }
  }

  override def attach(parent: Element): Unit = {
    parent.appendChild(rendered)
    tabs.foreach(attachTab)
    activateFirstTab()
  }

  private[this] def activateFirstTab(): Unit = {
    tabs.headOption.foreach(_.tab.querySelector("a").asInstanceOf[HTMLAnchorElement].click())
  }
}
