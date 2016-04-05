package akkaviz.frontend.components

import org.scalajs.dom.{Element, Event}
import rx.Var

import scalatags.JsDom.all._

class UnconnectedOnOff(status: Var[Boolean]) extends OnOffWithLabel with Component {
  lbl.innerHTML = "Show actors without any connections on graph"
  inp.onchange = { e: Event =>
    status() = inp.checked
  }

  override def attach(parent: Element): Unit = {
    val elem = Seq[Frag](
      div(`class` := "panel-body", stateBtn)
    ).render
    parent.appendChild(elem)
  }
}