package akkaviz.frontend.components

import akkaviz.frontend.ActorRepository.ActorState
import org.scalajs.dom.{Element => domElement}
import rx.Var

import scalatags.JsDom.all._

trait ClosableTab extends Tab {
  def onClose(): Unit = {
    tab.parentNode.removeChild(tab)
    tabBody.parentNode.removeChild(tabBody)
  }

  override def attach(tabbedPane: domElement): Unit = {
    super.attach(tabbedPane)
    tab.appendChild(a(cls:="glyphicon glyphicon-remove", href:="#", float.left, onclick := onClose _).render)
  }
}

trait Tab extends Component {
  def name: String

  def tabId: String

  lazy val tab = li(a(href:=s"#$tabId", "data-toggle".attr:="tab", s"$name", float.left)).render

  lazy val tabBody = div(`class`:="tab-pane panel panel-default ", id:=s"$tabId").render

  override def attach(tabbedPane: domElement): Unit = {
    tabbedPane.querySelector("ul.nav-tabs").appendChild(tab)
    tabbedPane.querySelector("div.tab-content").appendChild(tabBody)
  }

}

class ActorStateTab(actorState: Var[ActorState]) extends ClosableTab {
  import akkaviz.frontend.PrettyJson._

  val name = actorState.now.path
  val tabId = s"actor-state-${actorState.now.path.replace("/", "-").filterNot(_ == ':')}"
  val stateObs = actorState.foreach(renderState(_))

  def renderState(state: ActorState) = {
    val rendered = div(cls := "panel-body",
      div(strong("Class: "), state.className.getOrElse[String]("Unknown class")),
      div(strong("Is dead: "), state.isDead.toString),
      div(strong("Internal state: "), pre(state.internalState.map(prettyPrintJson).getOrElse[String]("Internal state unknown"))),
      div(strong("Is FSM: "), state.fsmState.isDefined.toString),
      state.fsmState.map[Frag] {
        fsm =>
          Seq(
            div(strong("FSM State: "), pre(prettyPrintJson(fsm.currentState))),
            div(strong("FSM Data: "), pre(prettyPrintJson(fsm.currentData)))
          )
      }.getOrElse(()),
      div(strong("Mailbox size: "), state.mailboxSize.map(_.toString).getOrElse[String]("Unknown")),
      div(strong("Last updated: "), state.lastUpdatedAt.toISOString())
    ).render

    tabBody.innerHTML = ""
    tabBody.appendChild(rendered)
  }


  override def onClose() = {
    super.onClose()
    stateObs.kill()
  }

}

