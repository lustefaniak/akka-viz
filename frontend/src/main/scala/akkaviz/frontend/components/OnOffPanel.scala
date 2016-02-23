package akkaviz.frontend.components

import akkaviz.frontend.PrettyJson
import org.scalajs.dom
import org.scalajs.dom.html.{Option => DomOption, _}
import org.scalajs.dom.{Element => domElement, Event}
import rx.{Ctx, Rx, Var}

import scalatags.JsDom.all._

class OnOffPanel(status: Var[MonitoringStatus])(implicit ctx: Ctx.Owner) extends Component with PrettyJson {

  lazy val messagePanelTitle = div(cls := "panel-heading", id := "messagespaneltitle", "Toggle reporting").render

  lazy val lbl = span().render
  lazy val inp = input(tpe := "checkbox").render

  inp.onchange = (d: Event) => {
    status() = Awaiting(Synced(inp.checked))
  }

  val statusTrigger = status.foreach {
    case UnknownYet =>
      lbl.innerHTML = "Awaiting server status"
      inp.disabled = true

    case Awaiting(s) =>
      lbl.innerHTML = s"Awaiting server confirmation for $s"
      inp.disabled = true
      inp.checked = s.asBoolean

    case synced: Synced =>
      inp.checked = synced.asBoolean
      inp.disabled = false
      lbl.innerHTML = s"Monitoring is <b>$synced</b>"

  }

  lazy val stateBtn = div(
    `class` := "togglebutton",
    label(
      inp, lbl
    )
  )

  override def render: Element = {
    Seq(
      div(`class` := "panel-body", stateBtn)
    ).render.asInstanceOf[Element]
  }
}

sealed trait MonitoringStatus
sealed trait Synced extends MonitoringStatus { def asBoolean: Boolean }

object Synced {
  def apply(b: Boolean) = if (b) On else Off
}

case object On extends Synced { val asBoolean = true }
case class Awaiting(target: Synced) extends MonitoringStatus
case object Off extends Synced { val asBoolean = false }
case object UnknownYet extends MonitoringStatus
