package akkaviz.frontend

import akkaviz.frontend.ActorRepository.FSMState
import akkaviz.frontend.ApiConnection.ApiUpstream
import akkaviz.frontend.components._
import akkaviz.protocol
import akkaviz.protocol._
import org.scalajs.dom.raw.{CloseEvent, ErrorEvent, MessageEvent}
import org.scalajs.dom.{Node, console, document}
import rx._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.scalajs.js.{JSApp, timers}
import scalatags.JsDom.all._

case class FsmTransition(fromStateClass: String, toStateClass: String)

object FrontendApp extends JSApp with Persistence
    with MailboxDisplay with PrettyJson with ManipulationsUI {

  @JSExport("toggleActor")
  def toggleActor(name: String) = actorSelector.toggleActor(name)

  private val repo = new ActorRepository()

  private def handleDownstream(messageReceived: (Received) => Unit)(messageEvent: MessageEvent): Unit = {
    val bb = TypedArrayBuffer.wrap(messageEvent.data.asInstanceOf[ArrayBuffer])
    val message: ApiServerMessage = protocol.IO.readServer(bb)

    message match {
      case ActorSystemCreated(system) =>

      case rcv: Received =>
        val sender = rcv.sender
        val receiver = rcv.receiver
        repo.addActorsToSeen(sender, receiver)
        messageReceived(rcv)
        graphView.ensureGraphLink(sender, receiver, FrontendUtil.shortActorName)

      case ac: AvailableClasses =>
        seenMessages() = ac.availableClasses.toSet

      case Spawned(child) =>
        repo.mutateActor(child) {
          _.copy(isDead = false)
        }

      case fsm: FSMTransition =>
        repo.mutateActor(fsm.ref) {
          _.copy(fsmState = FSMState(fsm.nextState, fsm.nextData))
        }

      case i: Instantiated =>
        repo.mutateActor(i.ref) {
          _.copy(className = i.clazz)
        }

      case CurrentActorState(ref, state) =>
        repo.mutateActor(ref) {
          _.copy(internalState = state)
        }

      case mb: MailboxStatus =>
        repo.mutateActor(mb.owner) {
          _.copy(mailboxSize = mb.size)
        }
        handleMailboxStatus(mb, graphView)

      case Killed(ref) =>
        repo.mutateActor(ref) {
          _.copy(isDead = true)
        }

      case af: ActorFailure =>
        thrownExceptions() = af +: thrownExceptions.now

      case SnapshotAvailable(live, deadActors, rcv) =>
        repo.addActorsToSeen(live: _*)
        deadActors.foreach {
          dead =>
            repo.mutateActor(dead) {
              _.copy(isDead = true)
            }
        }
        rcv.foreach {
          case (from, to) => graphView.ensureGraphLink(from, to, FrontendUtil.shortActorName)
        }

      case ReceiveDelaySet(duration) =>
        delayMillis() = duration.toMillis.toInt

      case ReportingEnabled =>
        monitoringStatus() = On

      case ReportingDisabled =>
        monitoringStatus() = Off

      case Ping => {}

    }
  }

  private val monitoringStatus = Var[MonitoringStatus](UnknownYet)
  private val selectedActors = persistedVar[Set[String]](Set(), "selectedActors")
  private val seenMessages = Var[Set[String]](Set())
  private val selectedMessages = persistedVar[Set[String]](Set(), "selectedMessages")
  private val thrownExceptions = Var[Seq[ActorFailure]](Seq())
  private val showUnconnected = Var[Boolean](false)
  private val actorSelector = new ActorSelector(repo.seenActors, selectedActors, repo.state, thrownExceptions)
  private val messageFilter = new MessageFilter(seenMessages, selectedMessages, selectedActors)
  private val messagesPanel = new MessagesPanel(selectedActors)
  private val monitoringOnOff = new MonitoringOnOff(monitoringStatus)
  private val connectionAlert = new Alert()
  private val unconnectedOnOff = new UnconnectedOnOff(showUnconnected)
  private val replTerminal = new ReplTerminal()
  private val graphView = new GraphView(showUnconnected)
  private val maxRetries = 10

  def main(): Unit = {

    repo.seenActors.triggerLater {
      repo.seenActors.now.foreach {
        actor =>
          val actorState = repo.state(actor).now
          graphView.ensureNodeExists(actor, actorState.label, js.Dictionary(("dead", actorState.isDead)))
      }
    }

    def setupApiConnection: Unit = {

      val connection: Future[ApiUpstream] = ApiConnection(
        FrontendUtil.webSocketUrl("stream"),
        upstream => {
          upstream.send(SetAllowedMessages(selectedMessages.now))
          upstream.send(ObserveActors(selectedActors.now))
        },
        handleDownstream(messagesPanel.messageReceived),
        maxRetries
      )

      connection.foreach { upstream =>
        connectionAlert.success("Connected!")
        connectionAlert.fadeOut()
        val triggers = Seq(

        selectedMessages.triggerLater {
          console.log(s"Will send allowedClasses: ${selectedMessages.now.mkString("[", ",", "]")}")
          upstream.send(SetAllowedMessages(selectedMessages.now))
        },

        selectedActors.trigger {
          console.log(s"Will send ObserveActors: ${selectedActors.now.mkString("[", ",", "]")}")
          upstream.send(ObserveActors(selectedActors.now))
        },

        delayMillis.triggerLater {
          import scala.concurrent.duration._
          upstream.send(SetReceiveDelay(delayMillis.now.millis))
        },

        monitoringStatus.triggerLater {
          monitoringStatus.now match {
            case Awaiting(s) =>
              console.log("monitoring status: ", monitoringStatus.now.toString)
              upstream.send(SetEnabled(s.asBoolean))
            case _ =>
              console.log("monitoring status: ", monitoringStatus.now.toString)
          }
        },
        )


        upstream.onclose = { ce: CloseEvent =>
          connectionAlert.warning("Reconnecting...")
          console.log("ws closed, retrying in 2 seconds")
          cleanUpAndRetry(triggers)
        }
        upstream.onerror = { ce: ErrorEvent =>
          connectionAlert.warning("Reconnecting...")
          console.log("ws error, retrying in 2 seconds")
          cleanUpAndRetry(triggers)
        }

        def cleanUpAndRetry(triggers: Seq[Obs]): Unit = {
          triggers.foreach(_.kill())
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

    document.body.appendChild(connectionAlert.render)
    insertComponent("#actorselection", actorSelector.render)
    insertComponent("#messagefiltering", messageFilter.render)
    insertComponent("#messagelist", messagesPanel.render)
    insertComponent("#receivedelay", receiveDelayPanel.render)
    insertComponent("#onoffsettings", monitoringOnOff.render)
    insertComponent("#graphsettings", unconnectedOnOff.render)
    insertComponent("#repl", replTerminal.render)
    //FIXME: insert GraphView here when it gets ported to scala

    DOMGlobalScope.$.material.init()

  }

  def insertComponent(parentSelector: String, component: Node): Node = {
    document.querySelector(parentSelector).appendChild(component)
  }
}