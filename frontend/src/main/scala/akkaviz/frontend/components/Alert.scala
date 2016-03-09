package akkaviz.frontend.components

import org.scalajs.dom.Element
import org.scalajs.dom.raw.MouseEvent

import scala.concurrent.duration._
import scala.scalajs.js.timers
import scalatags.JsDom.all._

class Alert extends Component {
  lazy val connectionAlert = div(
    "Connecting...",
    cls := "alert fade in",
    id := "connectionStatus",
    position.fixed, right := 0.px
  ).render

  override def attach(parent: Element): Unit = {
    parent.appendChild(connectionAlert)
  }

  def success(msg: String, fn: MouseEvent => Unit = _ => {}) = {
    connectionAlert.onclick = fn
    connectionAlert.innerHTML = msg
    connectionAlert.classList.remove("alert-warning")
    connectionAlert.classList.remove("alert-danger")
    connectionAlert.classList.add("alert-success")
    connectionAlert.classList.add("in")
  }

  def warning(msg: String, fn: MouseEvent => Unit = _ => {}) = {
    connectionAlert.onclick = fn
    connectionAlert.innerHTML = msg
    connectionAlert.classList.remove("alert-success")
    connectionAlert.classList.remove("alert-danger")
    connectionAlert.classList.add("alert-warning")
    connectionAlert.classList.add("in")
  }

  def error(msg: String, fn: MouseEvent => Unit = _ => {}) = {
    connectionAlert.onclick = fn
    connectionAlert.innerHTML = msg
    connectionAlert.classList.remove("alert-success")
    connectionAlert.classList.remove("alert-warning")
    connectionAlert.classList.add("alert-danger")
    connectionAlert.classList.add("in")
  }

  def fadeOut(after: FiniteDuration = 2.seconds) = timers.setTimeout(after) {
    connectionAlert.classList.remove("in")
  }

}
