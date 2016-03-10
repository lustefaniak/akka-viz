package akkaviz.frontend.components

import akkaviz.frontend.ActorRepository.ActorState
import akkaviz.protocol
import org.scalajs.dom.{Element => domElement, _}
import rx.{Ctx, Rx, Var}

import scalatags.JsDom.all._

trait ClosableTab extends Tab {
  def onClose(): Unit = {
    tab.parentNode.removeChild(tab)
    tabBody.parentNode.removeChild(tabBody)
  }

  override def attach(tabbedPane: domElement): Unit = {
    super.attach(tabbedPane)
    tab.appendChild(a(cls := "glyphicon glyphicon-remove", href := "#", float.left, onclick := onClose _).render)
  }
}

trait Tab extends Component {
  def name: String

  def tabId: String

  lazy val activateA = a(href := s"#$tabId", "data-toggle".attr := "tab", s"$name", float.left).render
  lazy val tab = li(activateA).render

  lazy val tabBody = div(`class` := "tab-pane panel panel-default ", id := s"$tabId").render

  override def attach(tabbedPane: domElement): Unit = {
    tabbedPane.querySelector("ul.nav-tabs").appendChild(tab)
    tabbedPane.querySelector("div.tab-content").appendChild(tabBody)
    activateA.click()
  }

}

class ActorStateTab(actorState: Var[ActorState], upstreamSend: protocol.ApiClientMessage => Unit)(implicit co: Ctx.Owner) extends ClosableTab {
  import scalatags.rx.all._
  import akkaviz.frontend.PrettyJson._
  import ActorStateTab._
  import akkaviz.frontend.PrettyJson._

  val name = actorState.now.path
  val tabId = stateTabId(actorState.now.path)

  renderState(actorState)

  def renderState(state: Var[ActorState]) = {

    lazy val fsmDiv = div(cls := s"fsm-graph", height := 250.px).render
    val fsmGraph = new FsmGraph(fsmDiv)

    val rendered = div(
      cls := "panel-body",
      refreshButton(state.path)(disableMaybe), killButton(state.path)(disableMaybe), poisonPillButton(state.path)(disableMaybe),
      fsmDiv,
      div(strong("Class: "), Rx(state().className.getOrElse[String]("Unknown class"))),
      div(strong("Is dead: "), Rx(state().isDead.toString)),
      div(strong("Internal state: "), pre(Rx(state().internalState.map(prettyPrintJson).getOrElse[String]("Internal state unknown")))),
      div(strong("Is FSM: "), Rx(state().fsmState.isDefined.toString)),
      div(Rx(state().fsmState.map { fs =>
        div(
          div(strong("FSM data:"), pre(prettyPrintJson(fs.currentData))),
          div(strong("FSM state:"), pre(prettyPrintJson(fs.currentState)))
        ).render
      }.getOrElse(div().render))),
      div(strong("Mailbox size: "), Rx(state().mailboxSize.map(_.toString).getOrElse[String]("Unknown"))),
      div(strong("Last updated: "), Rx(state().lastUpdatedAt.toISOString()))
    ).render

    tabBody.appendChild(rendered)
    state.map(_.fsmTransitions).foreach(fsmGraph.displayFsm)
  }

  override def onClose(): Unit = {
    super.onClose()
  }

  private[this] def refreshButton(actorRef: String) =
    a(cls := "btn btn-default", href := "#", role := "button", float.right,
      span(
        `class` := "imgbtn glyphicon glyphicon-refresh", " "
      ),
      onclick := { () =>
        upstreamSend(protocol.RefreshInternalState(actorRef))
      },
      "Refresh state")

  private[this] def killButton(actorRef: String) =
    a(cls := "btn btn-default", href := "#", role := "button", float.right,
      span(`class` := "glyphicons glyphicons-remove-sign"),
      onclick := { () =>
        upstreamSend(protocol.KillActor(actorRef))
      },
      "Kill")

  private[this] def poisonPillButton(actorRef: String) =
    a(cls := "btn btn-default", href := "#", role := "button", float.right,
      span(`class` := "glyphicons glyphicons-lab"),
      onclick := { () =>
        upstreamSend(protocol.PoisonPillActor(actorRef))
      },
      "PoisonPill")
}

object ActorStateTab {
  def stateTabId(path: String): String = {
    s"actor-state-${path.replaceAll("[\\/|\\.|\\\\|\\$]", "-").filterNot(_ == ':')}"
  }
}

