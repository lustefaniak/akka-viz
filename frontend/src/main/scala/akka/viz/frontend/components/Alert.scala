package akka.viz.frontend.components

import org.scalajs.dom
import org.scalajs.dom.html.Element

import scala.concurrent.duration._
import scalatags.JsDom.all._

class Alert extends Component {
  lazy val connectionAlert = div(
    "Connecting...",
    cls := "alert fade in",
    id := "connectionStatus",
    position.fixed, right := 0.px
  ).render

  override def render: Element = connectionAlert

  def success(msg: String) = {
    connectionAlert.innerHTML = msg
    connectionAlert.classList.remove("alert-warning")
    connectionAlert.classList.remove("alert-error")
    connectionAlert.classList.add("alert-success")
    connectionAlert.classList.add("in")

  }

  def warning(msg: String) = {
    connectionAlert.innerHTML = msg
    connectionAlert.classList.remove("alert-success")
    connectionAlert.classList.remove("alert-error")
    connectionAlert.classList.add("alert-warning")
    connectionAlert.classList.add("in")
  }

  def error(msg: String) = {
    connectionAlert.innerHTML = msg
    connectionAlert.classList.remove("alert-success")
    connectionAlert.classList.remove("alert-warning")
    connectionAlert.classList.add("alert-danger")
    connectionAlert.classList.add("in")
  }

  def fadeOut(after: Duration = 2.seconds) = dom.setTimeout(() => connectionAlert.classList.remove("in"), after.toMillis)

}
