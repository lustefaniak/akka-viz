package akkaviz.frontend.components

import org.scalajs.dom.{Element => domElement}

import scalatags.JsDom.all._

trait ClosableTab extends Tab {
  def onClose(): Unit = {}

  override def attach(tabbedPane: domElement): Unit = {
    super.attach(tabbedPane)
    tab.appendChild(a(cls := "glyphicon glyphicon-remove close-tab", href := "#", float.left, onclick := onClose _).render)
  }
}
