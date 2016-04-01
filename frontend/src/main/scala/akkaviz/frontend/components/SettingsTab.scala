package akkaviz.frontend.components

import rx.Var

import scalatags.JsDom.all._

class SettingsTab(
    monitoringStatus: Var[MonitoringStatus],
    showUnconnected: Var[Boolean]
) extends Tab {

  val monitoringOnOff = new MonitoringOnOff(monitoringStatus)
  val unconnectedOnOff = new UnconnectedOnOff(showUnconnected)

  override def name: String = "Settings"

  override def tabId: String = "globalsettings"

  override def onCreate(): Unit = {
    monitoringOnOff.attach(tabBody)
    unconnectedOnOff.attach(tabBody)
    val graphSettings = div(id := "graphsettings").render
    tabBody.appendChild(graphSettings)
  }

}
