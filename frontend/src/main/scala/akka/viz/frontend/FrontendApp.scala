package akka.viz.frontend

import akka.viz.frontend.FrontendUtil._
import akka.viz.frontend.components.{ActorSelector, MessageFilter, MessagesPanel, OnOffPanel}
import akka.viz.protocol._
import org.scalajs.dom
import org.scalajs.dom.raw.{ErrorEvent, CloseEvent, MessageEvent}
import org.scalajs.dom.{console, document}
import rx._
import upickle.default._

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class FsmTransition(fromStateClass: String, toStateClass: String)

object FrontendApp extends JSApp with Persistence
  with MailboxDisplay with PrettyJson with ManipulationsUI {

  val createdLinks = scala.collection.mutable.Set[String]()
  val graph = DOMGlobalScope.graph

  private val _actorClasses: mutable.Map[String, Var[js.UndefOr[String]]] = mutable.Map()
  private val _currentActorState = mutable.Map[String, Var[js.UndefOr[String]]]()
  private val _eventsEnabled = Var(false)

  def actorClasses(actor: String) = _actorClasses.getOrElseUpdate(actor, Var(js.undefined))

  def currentActorState(actor: String) = _currentActorState.getOrElseUpdate(actor, Var(js.undefined))

  val deadActors = mutable.Set[String]()

  private def handleDownstream(messageReceived: (Received) => Unit)(messageEvent: MessageEvent): Unit = {
    val message: ApiServerMessage = ApiMessages.read(messageEvent.data.asInstanceOf[String])

    message match {
      case rcv: Received =>
        val sender = rcv.sender
        val receiver = rcv.receiver
        addActorsToSeen(sender, receiver)
        messageReceived(rcv)
        ensureGraphLink(sender, receiver)

      case ac: AvailableClasses =>
        seenMessages() = ac.availableClasses.toSet

      case Spawned(child, parent) =>
        deadActors -= child
        addActorsToSeen(child, parent)

      case fsm: FSMTransition =>
      //TODO: handle in UI

      case i: Instantiated =>
        val actor = i.ref
        actorClasses(actor)() = i.clazz

      case CurrentActorState(ref, state) =>
        currentActorState(ref)() = state

      case mb: MailboxStatus =>
        handleMailboxStatus(mb)

      case ReceiveDelaySet(duration) =>
        delayMillis() = duration.toMillis.toInt

      case ReportingEnabled =>
        _eventsEnabled() = true

      case ReportingDisabled =>
        _eventsEnabled() = false

      case Killed(ref) =>
        deadActors += ref
        seenActors.recalc()

      case SnapshotAvailable(live, dead, childrenOf, rcv) =>
        addActorsToSeen(live: _*)
        deadActors ++= dead
        for {
          (from, to) <- rcv
        } ensureGraphLink(from, to)
        seenActors.recalc()

      case Ping => {}
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
    seenActors.now.foreach {
      actor =>
        val isDead = deadActors.contains(actor)
        graph.addNode(actor, js.Dictionary(("dead", isDead)))
    }
  }

  private def addActorsToSeen(actorName: String*): Unit = {
    val previouslySeen = seenActors.now
    val newSeen = previouslySeen ++ actorName.filterNot(previouslySeen(_))
    if (previouslySeen.size != newSeen.size)
      seenActors() = newSeen
  }

  val actorSelector = new ActorSelector(seenActors, selectedActors, currentActorState, actorClasses)
  val messageFilter = new MessageFilter(seenMessages, selectedMessages, selectedActors)
  val messagesPanel = new MessagesPanel(selectedActors)
  val userIsEnabled = Var(false)
  val onOffPanel = new OnOffPanel(_eventsEnabled, userIsEnabled)

  @JSExport("toggleActor")
  def toggleActor(name: String) = actorSelector.toggleActor(name)

  def main(): Unit = {

    def setupApiConnection: Unit = {
      ApiConnection(
        webSocketUrl("stream"),
        handleDownstream(messagesPanel.messageReceived),
        maxRetries = 10
      ).foreach { upstream => // todo warn user if couldn't establish connection at all

        selectedMessages.triggerLater {
          console.log(s"Will send allowedClasses: ${selectedMessages.now.mkString("[", ",", "]")}")
          import upickle.default._
          upstream.send(write(SetAllowedMessages(selectedMessages.now)))
        }

        selectedActors.trigger {
          console.log(s"Will send ObserveActors: ${selectedActors.now.mkString("[", ",", "]")}")
          import upickle.default._
          upstream.send(write(ObserveActors(selectedActors.now)))
        }

        delayMillis.triggerLater {
          import scala.concurrent.duration._
          upstream.send(write(SetReceiveDelay(delayMillis.now.millis)))
        }

        userIsEnabled.triggerLater {
          upstream.send(write(SetEnabled(userIsEnabled.now)))
        }

        // todo warn user when retry is in progress
        upstream.onclose = { ce: CloseEvent =>
          console.log("ws closed, retrying in 2 seconds")
          dom.setTimeout(() => setupApiConnection, 2000)
        }
        upstream.onerror = { ce: ErrorEvent =>
          console.log("ws error, retrying in 2 seconds")
          dom.setTimeout(() => setupApiConnection, 2000)
        }
      }
    }

    setupApiConnection

    document.querySelector("#actorselection").appendChild(actorSelector.render)
    document.querySelector("#messagefiltering").appendChild(messageFilter.render)
    document.querySelector("#messagelist").appendChild(messagesPanel.render)
    document.querySelector("#receivedelay").appendChild(receiveDelayPanel.render)
    document.querySelector("#onoffsettings").appendChild(onOffPanel.render)

    DOMGlobalScope.$.material.init()

  }
}