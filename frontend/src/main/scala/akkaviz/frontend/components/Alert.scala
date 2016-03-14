package akkaviz.frontend.components

import org.scalajs.dom.Element
import org.scalajs.dom.raw.MouseEvent

import scala.concurrent.duration._
import scala.scalajs.js.timers
import scalatags.JsDom.all._

class Alert extends Component {
  private[this] lazy val connectionAlert = div(
    "Connecting...",
    cls := "alert fade in",
    id := "connectionStatus",
    position.fixed, right := 0.px, zIndex := 90210
  ).render

  override def attach(parent: Element): Unit = {
    parent.appendChild(connectionAlert)
  }

  private[this] def updateElement(msg: String, currentClass: String, fn: MouseEvent => Unit = _ => {}): Unit = {
    connectionAlert.onclick = fn
    connectionAlert.innerHTML = msg
    connectionAlert.classList.remove("alert-warning")
    connectionAlert.classList.remove("alert-danger")
    connectionAlert.classList.remove("alert-success")
    connectionAlert.classList.add(currentClass)
    connectionAlert.classList.add("in")
  }

  def success(msg: String, fn: MouseEvent => Unit = _ => {}): Unit = {
    updateElement(msg, "alert-success", fn)
  }

  def warning(msg: String, fn: MouseEvent => Unit = _ => {}): Unit = {
    updateElement(msg, "alert-warning", fn)
  }

  def error(msg: String, fn: MouseEvent => Unit = _ => {}): Unit = {
    updateElement(msg, "alert-danger", fn)
  }

  def fadeOut(after: FiniteDuration = 2.seconds) = timers.setTimeout(after) {
    connectionAlert.classList.remove("in")
  }

}
