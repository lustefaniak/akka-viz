package akkaviz.frontend.components

import akkaviz.frontend.PrettyJson
import org.scalajs.dom
import org.scalajs.dom.html.{Option => DomOption, _}
import org.scalajs.dom.{Element => domElement, Event}
import rx.{Ctx, Rx, Var}

import scalatags.JsDom.all._

class OnOffPanel(serverIsEnabled: Var[Option[Boolean]], userIsEnabled: Var[Boolean])(implicit ctx: Ctx.Owner) extends Component with PrettyJson {

  lazy val messagePanelTitle = div(cls := "panel-heading", id := "messagespaneltitle", "Toggle reporting").render

  lazy val lbl = span().render
  lazy val inp = input(tpe := "checkbox").render

  inp.onchange = (d: Event) => {
    userIsEnabled() = inp.checked
    lbl.innerHTML = "Awaiting Server confirmation"
    inp.disabled = true
  }

  val awaitingConfirmation = Rx((serverIsEnabled(), userIsEnabled())).foreach {
    case (None, _) =>
      lbl.innerHTML = "Awaiting Server status"
      lbl.disabled = true
    case (Some(srv), _) =>
      inp.checked = srv
      inp.disabled = false
      if (srv) {
        lbl.innerHTML = "Monitoring <b>ENABLED</b> on the Server"
      } else {
        lbl.innerHTML = "Monitoring <b>DISABLED</b> on the Server"
      }
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

