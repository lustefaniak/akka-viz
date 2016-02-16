package akkaviz.frontend.components

import akkaviz.frontend.PrettyJson
import org.scalajs.dom.html._
import org.scalajs.dom.{Element => domElement, Event}
import rx.Var

import scalatags.JsDom.all._

class OnOffPanel(serverIsEnabled: Var[Boolean], userIsEnabled: Var[Boolean]) extends Component with PrettyJson {

  lazy val messagePanelTitle = div(cls := "panel-heading", id := "messagespaneltitle", "Toggle reporting").render

  lazy val lbl = span().render
  lazy val inp = input(tpe := "checkbox").render
  serverIsEnabled.trigger {
    inp.checked = serverIsEnabled.now
    if (serverIsEnabled.now) {
      lbl.innerHTML = "Monitoring <b>ENABLED</b> on the Server"
    } else {
      lbl.innerHTML = "Monitoring <b>DISABLED</b> on the Server"
    }
  }
  inp.onchange = (d: Event) => {
    userIsEnabled() = inp.checked
    lbl.innerHTML = "Awaiting Server confirmation"
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

