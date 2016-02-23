package akkaviz.frontend

import akkaviz.frontend.components._
import akkaviz.protocol
import akkaviz.protocol._
import org.scalajs.dom.raw.{CloseEvent, ErrorEvent, MessageEvent, WebSocket}
import org.scalajs.dom.{Node, console, document}
import rx._
import upickle.default._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.{JSApp, timers}
import scalatags.JsDom.all._

case class FsmTransition(fromStateClass: String, toStateClass: String)

object FrontendApp extends JSApp with Persistence
    with MailboxDisplay with PrettyJson with ManipulationsUI
    with ReplTerminal {

  private val createdLinks = js.Dictionary[Unit]()
  private val graph = DOMGlobalScope.graph

  private val _actorClasses = js.Dictionary[Var[js.UndefOr[String]]]()
  private val _currentActorState = js.Dictionary[Var[js.UndefOr[String]]]()

  private def actorClasses(actor: String) = _actorClasses.getOrElseUpdate(actor, Var(js.undefined))

  private def currentActorState(actor: String) = _currentActorState.getOrElseUpdate(actor, Var(js.undefined))

  private val deadActors = mutable.Set[String]()
  private val monitoringStatus = Var[MonitoringStatus](UnknownYet)

  private def handleDownstream(messageReceived: (Received) => Unit)(messageEvent: MessageEvent): Unit = {
    val message: ApiServerMessage = protocol.IO.readServer(messageEvent.data.asInstanceOf[String])

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
        monitoringStatus() = On

      case ReportingDisabled =>
        monitoringStatus() = Off

      case Killed(ref) =>
        addActorsToSeen(ref)
        deadActors += ref
        seenActors.recalc()

      case af: ActorFailure =>
        addActorsToSeen(af.actorRef)
        thrownExceptions() = af +: thrownExceptions.now

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
    if (!createdLinks.contains(linkId)) {
      createdLinks.update(linkId, ())
      graph.beginUpdate()
      graph.addLink(sender, receiver, linkId)
      graph.endUpdate()
    }
  }

  private val seenActors = Var[Set[String]](Set())
  private val selectedActors = persistedVar[Set[String]](Set(), "selectedActors")
  private val seenMessages = Var[Set[String]](Set())
  private val selectedMessages = persistedVar[Set[String]](Set(), "selectedMessages")
  private val thrownExceptions = Var[Seq[ActorFailure]](Seq())
  private val showUnconnected = Var[Boolean](false)

  private val addNodesObs = Rx((showUnconnected(), seenActors())).trigger {
    seenActors.now.foreach {
      actor =>
        if (showUnconnected.now || createdLinks.exists(_._1.split("->").contains(actor))) {
          val isDead = deadActors.contains(actor)
          graph.addNode(actor, js.Dictionary(("dead", isDead)))
        } else {
          graph.removeNode(actor)
        }
    }
  }

  private def addActorsToSeen(actorName: String*): Unit = {
    val previouslySeen = seenActors.now
    val newSeen = previouslySeen ++ actorName.filterNot(previouslySeen(_))
    if (previouslySeen.size != newSeen.size)
      seenActors() = newSeen
  }

  private val actorSelector = new ActorSelector(seenActors, selectedActors, currentActorState, actorClasses, thrownExceptions)
  private val messageFilter = new MessageFilter(seenMessages, selectedMessages, selectedActors)
  private val messagesPanel = new MessagesPanel(selectedActors)
  private val monitoringOnOff = new MonitoringOnOff(monitoringStatus)
  private val connectionAlert = new Alert()
  private val unconnectedOnOff = new UnconnectedOnOff(showUnconnected)

  @JSExport("toggleActor")
  def toggleActor(name: String) = actorSelector.toggleActor(name)

  private val maxRetries = 10

  def main(): Unit = {

    def setupApiConnection: Unit = {

      val connection: Future[WebSocket] = ApiConnection(
        FrontendUtil.webSocketUrl("stream"),
        upstream => {
          upstream.send(write(SetAllowedMessages(selectedMessages.now)))
          upstream.send(write(ObserveActors(selectedActors.now)))
        },
        handleDownstream(messagesPanel.messageReceived),
        maxRetries
      )

      connection.foreach { upstream =>
        connectionAlert.success("Connected!")
        connectionAlert.fadeOut()

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

        monitoringStatus.triggerLater {
          monitoringStatus.now match {
            case Awaiting(s) =>
              console.log("monitoring status: ", monitoringStatus.now.toString)
              val write1: String = write(SetEnabled(s.asBoolean))
              console.log(write1)
              upstream.send(write1)
            case _ =>
              console.log("monitoring status: ", monitoringStatus.now.toString)
          }
        }

        upstream.onclose = { ce: CloseEvent =>
          connectionAlert.warning("Reconnecting...")
          console.log("ws closed, retrying in 2 seconds")
          timers.setTimeout(2.seconds) {
            setupApiConnection
          }
        }
        upstream.onerror = { ce: ErrorEvent =>
          connectionAlert.warning("Reconnecting...")
          console.log("ws error, retrying in 2 seconds")
          timers.setTimeout(2.seconds) {
            setupApiConnection
          }
        }
      }

      connection.onFailure {
        case _ => connectionAlert.error(s"Connection failed after $maxRetries retries. Try reloading the page.")
      }
    }

    setupApiConnection

    setupReplTerminal(document.getElementById("repl"))

    document.body.appendChild(connectionAlert.render)
    insertComponent("#actorselection", actorSelector.render)
    insertComponent("#messagefiltering", messageFilter.render)
    insertComponent("#messagelist", messagesPanel.render)
    insertComponent("#receivedelay", receiveDelayPanel.render)
    insertComponent("#onoffsettings", monitoringOnOff.render)
    insertComponent("#graphsettings", unconnectedOnOff.render)
    DOMGlobalScope.$.material.init()

  }

  def insertComponent(parentSelector: String, component: Node): Node = {
    document.querySelector(parentSelector).appendChild(component)
  }
}