package akkaviz.frontend.components

import akkaviz.frontend.ActorRepository.ActorState
import akkaviz.frontend.vis.{Group, Item, DataSet, Graph2d}
import akkaviz.protocol
import akkaviz.protocol.ThroughputMeasurement
import org.scalajs.dom.{Element => domElement, _}
import rx.{Ctx, Rx, Var}

import scala.scalajs.js
import scala.scalajs.js.Date
import scalatags.JsDom.all._

trait ClosableTab extends Tab {
  tab.appendChild(a(cls := "glyphicon glyphicon-remove close-tab", href := "#", float.left, onclick := onClose _).render)

  def onClose(): Unit = {}

  override def attach(tabbedPane: domElement): Unit = {
    super.attach(tabbedPane)
  }
}

trait Tab extends Component {
  def name: String

  def tabId: String

  def isActive: Boolean = tab.classList.contains("active")

  lazy val activateA = a(href := s"#$tabId", "data-toggle".attr := "tab", s"$name", float.left).render
  lazy val tab = li(activateA).render

  lazy val tabBody = div(`class` := "tab-pane panel panel-default ", id := s"$tabId").render

  override def attach(tabbedPane: domElement): Unit = {
    tabbedPane.querySelector("ul.nav-tabs").appendChild(tab)
    tabbedPane.querySelector("div.tab-content").appendChild(tabBody)
  }

}

class ActorStateTab(actorState: Var[ActorState], upstreamSend: protocol.ApiClientMessage => Unit)(implicit co: Ctx.Owner) extends ClosableTab {
  import scalatags.rx.all._
  import ActorStateTab._
  import akkaviz.frontend.PrettyJson._

  val name = actorState.now.path
  val tabId = stateTabId(actorState.now.path)

  renderState(actorState)

  private[this] def renderState(state: Var[ActorState]) = {

    lazy val fsmDiv = div(cls := s"fsm-graph", height := 250.px, clear.both).render
    def disableMaybe(isDead: Boolean): Modifier = if (isDead) disabled := "disabled" else ()

    val rendered = div(
      cls := "panel-body",
      div(state.map(_.isDead).map { isDead =>
        div(
          refreshButton(state.now.path)(disableMaybe(isDead)),
          killButton(state.now.path)(disableMaybe(isDead)),
          poisonPillButton(state.now.path)(disableMaybe(isDead)),
          clear.both
        ).render
      }),
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
    val fsmGraph = new FsmGraph(fsmDiv)
    state.map(_.fsmTransitions).foreach(fsmGraph.displayFsm)
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

class ThroughputGraphViewTab extends Tab {
  import scala.concurrent.duration._

  override def name: String = "Throughput"

  override def tabId: String = "throughput-graph-tab"

  private[this] val items = new DataSet[Item]()
  private[this] val groups = new DataSet[Group]()

  val graphContainer = div(id := "thr-graph-container", width := 100.pct).render
  val options = js.Dynamic.literal(
    start = js.Date.now(),
    end = js.Date.now() + 2.minutes.toMillis
  )
  val graph = new Graph2d(graphContainer, items, groups, options)

  tabBody.appendChild(graphContainer)

  def addMeasurement(tm: ThroughputMeasurement): Unit = {
    val group = new Group(tm.actorRef, tm.actorRef)
    val date = new Date(js.Date.parse(tm.timestamp))
    val item = new Item(date, tm.msgPerSecond, tm.actorRef)
    groups.update(group)
    items.add(item)
  }
}