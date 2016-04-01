package akkaviz.frontend.components

import org.scalajs.dom.Element
import org.scalajs.dom.raw.HTMLAnchorElement

import scalatags.JsDom.all._

class TabMenu(elemId: String, initTabs: Tab*) extends Component {

  private[this] val navbar = ul(cls := "nav nav-tabs")
  private[this] val tabContent = div(cls := "tab-content")
  private[this] val rendered: Element = div(id := elemId, cls := "menu tab-container", navbar, tabContent).render

  private[this] var tabs: Seq[Tab] = Seq()

  def attachTab(tab: Tab): Unit = {
    tabs :+= tab
    tab.attach(rendered)
    tab.onCreate()
    if (tabs.size == 1)
      activate(tab)
  }

  override def attach(parent: Element): Unit = {
    parent.appendChild(rendered)
    initTabs.foreach(attachTab)
    tabs.headOption.foreach(activate)
  }

  def activate(tab: Tab): Unit = {
    tab.tab.querySelector("a").asInstanceOf[HTMLAnchorElement].click()
  }
}
