package akkaviz.frontend.components

import akkaviz.frontend.ActorPath
import akkaviz.frontend.ActorRepository.ActorState
import akkaviz.frontend.vis._
import akkaviz.protocol
import akkaviz.protocol.ThroughputMeasurement
import org.scalajs.dom.html.{Input, UList}
import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.dom.{Element => domElement, _}
import rx.{Ctx, Rx, Var}

import scala.scalajs.js
import scala.scalajs.js.{|, Date}
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

class ActorStateTab(
    actorState: Var[ActorState],
    upstreamSend: protocol.ApiClientMessage => Unit,
    actorMessagesOpener: (ActorPath) => Unit
)(implicit co: Ctx.Owner) extends ClosableTab {

  import ActorStateTab._
  import akkaviz.frontend.PrettyJson._

  import scalatags.rx.all._

  val name = actorState.now.path
  val tabId = stateTabId(actorState.now.path)

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
          actorMessagesButton(state.now.path),
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

  private[this] def refreshButton(actorRef: ActorPath) =
    a(cls := "btn btn-default", href := "#", role := "button", float.right,
      span(
        `class` := "imgbtn glyphicon glyphicon-refresh", " "
      ),
      onclick := { () =>
        upstreamSend(protocol.RefreshInternalState(actorRef))
      },
      "Refresh state")

  private[this] def killButton(actorRef: ActorPath) =
    a(cls := "btn btn-default", href := "#", role := "button", float.right,
      span(`class` := "glyphicons glyphicons-remove-sign"),
      onclick := { () =>
        upstreamSend(protocol.KillActor(actorRef))
      },
      "Kill")

  private[this] def poisonPillButton(actorRef: ActorPath) =
    a(cls := "btn btn-default", href := "#", role := "button", float.right,
      span(`class` := "glyphicons glyphicons-lab"),
      onclick := { () =>
        upstreamSend(protocol.PoisonPillActor(actorRef))
      },
      "PoisonPill")

  private[this] def actorMessagesButton(actorRef: ActorPath) =
    a(cls := "btn btn-default", href := "#", role := "button", float.right,
      onclick := { () =>
        actorMessagesOpener(actorRef)
      },
      "All messages")

  override def onCreate(): Unit = {
    renderState(actorState)
  }
}

object ActorStateTab {
  def stateTabId(path: ActorPath): String = {
    s"actor-state-${path.replaceAll("[\\/|\\.|\\\\|\\$]", "-").filterNot(_ == ':')}"
  }
}

class ThroughputGraphViewTab(implicit ctx: Ctx.Owner) extends Tab {
  import scala.concurrent.duration._
  import scalatags.rx.all._

  override def name: String = "Throughput"

  override def tabId: String = "throughput-graph-tab"

  private[this] val items = new DataSet[Item]()
  private[this] val groups = new DataSet[Group]()
  private[this] val groupVisibility = Var[Map[String, Boolean]](Map.empty)

  val graphContainer = div(id := "thr-graph-container", width := 100.pct).render
  val options = js.Dynamic.literal(
    start = js.Date.now(),
    end = js.Date.now() + 2.minutes.toMillis,
    interpolation = false
  )
  val graph = new Graph2d(graphContainer, items, groups, options)

  private[this] val rxElement = Rx {
    ul(groupVisibility().map { v =>
      li(input(tpe:="checkbox", if (v._2) checked else ()), v._1, onclick := {() => groupVisibility() = groupVisibility.now.updated(v._1, !v._2)})
    }.toSeq).render
  }

  val selector = div(rxElement).render

  tabBody.appendChild(graphContainer)
  tabBody.appendChild(selector)

  def addMeasurement(tm: ThroughputMeasurement): Unit = {
    val group = new Group(tm.actorRef, tm.actorRef)
    val date = new Date(js.Date.parse(tm.timestamp))
    val item = new Item(date, tm.msgPerSecond, tm.actorRef)
    removeOldItems()
    groups.update(group)
    items.add(item)
  }

  groups.on("add", { (event: String, p: Properties[Group], sender: String|Double) =>
    groupVisibility() = groupVisibility.now ++ (p.items.map(_ -> true))
  })

  private[this] def removeOldItems(): Unit = {
    val range = graph.getWindow()
    val interval = range.end.valueOf() - range.start.valueOf()

    val oldIds = items.getIds(js.Dynamic.literal(filter = { (item: Item) =>
      item.x.valueOf() < (range.start.valueOf() - interval)
    }))
    items.remove(oldIds)
  }

  private[this] val groupUpdate = groupVisibility.triggerLater {
    val g = groupVisibility.now
    val options = js.Dynamic.literal(groups = js.Dynamic.literal(
      visibility = js.Dictionary[Boolean](g.toSeq : _*)))
    console.log(options)
    graph.setOptions(options)
  }

}