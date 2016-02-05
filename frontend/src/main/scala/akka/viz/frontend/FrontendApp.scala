package akka.viz.frontend

import akka.viz.frontend.components.{MessageFilter, ActorSelector}
import akka.viz.protocol._
import org.scalajs.dom.html.Input
import org.scalajs.dom.{onclick => _, raw => _, _}
import rx._
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.{ThisFunction0, JSApp, JSON}
import scala.util.Try
import scalatags.JsDom.all._
import upickle.default._
import FrontendUtil._

case class FsmTransition(fromStateClass: String, toStateClass: String)

object FrontendApp extends JSApp with Persistence
    with MailboxDisplay with PrettyJson with ManipulationsUI {

  val createdLinks = scala.collection.mutable.Set[String]()
  val graph = DOMGlobalScope.graph

  val _actorClasses: mutable.Map[String, Var[js.UndefOr[String]]] = mutable.Map()

  def actorClasses(actor: String) = _actorClasses.getOrElseUpdate(actor, Var(js.undefined))

  val fsmTransitions = mutable.Map[String, mutable.Set[FsmTransition]]()
  val currentActorState = mutable.Map[String, String]()

  private def handleDownstream(messageEvent: MessageEvent): Unit = {
    val message: ApiServerMessage = ApiMessages.read(messageEvent.data.asInstanceOf[String])

    message match {
      case rcv: Received =>
        val sender = actorName(rcv.sender)
        val receiver = actorName(rcv.receiver)
        addActorsToSeen(sender, receiver)
        messageReceived(rcv)
        ensureGraphLink(sender, receiver)

      case ac: AvailableClasses =>
        seenMessages() = ac.availableClasses.toSet

      case Spawned(child, parent) =>
        addActorsToSeen(actorName(child), actorName(parent))

      case fsm: FSMTransition =>
        val actor = actorName(fsm.ref)
        //FIXME: subscribe for data
        fsmTransitions.getOrElseUpdate(actor, mutable.Set()) += FsmTransition(fsm.currentStateClass, fsm.nextStateClass)
        currentActorState.update(actor, """{"state": ${fsm.nextState}, "data":${fsm.nextData}}""")

      case i: Instantiated =>
        val actor = actorName(i.ref)
        actorClasses(actor)() = i.clazz

      case CurrentActorState(ref, state) =>
        currentActorState.update(actorName(ref), state)

      case mb: MailboxStatus =>
        handleMailboxStatus(mb)

      case ReceiveDelaySet(duration) =>
        delayMillis() = duration.toMillis.toInt
    }
  }

  private def ensureGraphLink(sender: String, receiver: String): Unit = {
    val linkId = s"${sender}->${receiver}"
    if (!createdLinks(linkId)) {
      createdLinks.add(linkId)
      graph.beginUpdate()
      graph.addLink(sender, receiver, linkId)
      graph.endUpdate()
    }
  }

  val seenActors = Var[Set[String]](Set())
  val selectedActors = persistedVar[Set[String]](Set(), "selectedActors")
  val seenMessages = Var[Set[String]](Set())
  val selectedMessages = persistedVar[Set[String]](Set(), "selectedMessages")

  val addNodesObs = seenActors.trigger {
    seenActors.now.foreach(graph.addNode(_, js.Dictionary.empty[js.Any]))
  }

  private def addActorsToSeen(actorName: String*): Unit = {
    val previouslySeen = seenActors.now
    val newSeen = previouslySeen ++ actorName.filterNot(previouslySeen(_))
    seenActors() = newSeen
  }

  lazy val messagesContent = document.getElementById("messagespanelbody").getElementsByTagName("tbody")(0).asInstanceOf[Element]

  private def messageReceived(rcv: Received): Unit = {
    def insert(e: Element): Unit = {
      messagesContent.appendChild(e)
    }
    val uid = rcv.eventId
    val sender = actorName(rcv.sender)
    val receiver = actorName(rcv.receiver)
    val selected = selectedActors.now

    if (selected.contains(sender) || selected.contains(receiver)) {

      val mainRow = tr(
        "data-toggle".attr := "collapse",
        "data-target".attr := s"#detail$uid",
        td(sender),
        td(receiver),
        td(rcv.payloadClass)
      )

      val payload: String = rcv.payload.getOrElse("")
      val detailsRow = tr(
        id := s"detail$uid",
        `class` := "collapse",
        td(
          colspan := 3,
          div(pre(prettyPrintJson(payload))) // FIXME: display formated lazily
        )
      )

      insert(mainRow.render)
      insert(detailsRow.render)
    }
  }

  val actorSelector = new ActorSelector(seenActors, selectedActors, currentActorState, actorClasses)
  val messageFilter = new MessageFilter(seenMessages, selectedMessages, selectedActors)

  @JSExport("toggleActor")
  def toggleActor(name: String) = actorSelector.toggleActor(name)

  def main(): Unit = {

    document.querySelector("#actorselection").appendChild(actorSelector.render)
    document.querySelector("#messagefiltering").appendChild(messageFilter.render)

    val upstream = ApiConnection(webSocketUrl("stream"), handleDownstream)

    selectedMessages.triggerLater {
      console.log(s"Will send allowedClasses: ${selectedMessages.now.mkString("[", ",", "]")}")
      import upickle.default._
      upstream.send(write(SetAllowedMessages(selectedMessages.now.toList)))

      selectedActors.trigger {
        if (selectedActors.now.isEmpty) {
          document.getElementById("messagespaneltitle").innerHTML = s"Select actor to show its messages"
        } else {
          document.getElementById("messagespaneltitle").innerHTML = s"Messages"
        }
        messagesContent.innerHTML = ""
      }
    }

    document.querySelector("#thebox").appendChild(receiveDelayPanel.render)
    delayMillis.triggerLater {
      import scala.concurrent.duration._

      upstream.send(write(SetReceiveDelay(delayMillis.now.millis)))
    }
  }
}