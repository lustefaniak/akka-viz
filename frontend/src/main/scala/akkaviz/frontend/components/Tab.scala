package akkaviz.frontend.components

import org.scalajs.dom.{Element => domElement}

import scalatags.JsDom.all._

trait Tab extends Component {
  def name: String

  def tabId: String

  def isActive: Boolean = tab.classList.contains("active")

  lazy val activateA = a(href := s"#$tabId", "data-toggle".attr := "tab", s"$name", float.left).render
  lazy val tab = li(activateA).render

  lazy val tabBody = div(`class` := "tab-pane panel panel-default ", id := s"$tabId").render

  override def attach(tabbedPane: domElement): Unit = {
    tabbedPane.querySelector("ul.nav-tabs").appendChild(tab)
    tabbedPane.querySelector("div.tab-content").appendChild(tabBody)
  }

  def onCreate(): Unit

}
