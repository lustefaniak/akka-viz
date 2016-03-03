package akkaviz.frontend

import akkaviz.frontend.ActorRepository.FSMState
import akkaviz.frontend.ApiConnection.ApiUpstream
import akkaviz.frontend.components.GraphView.LinkType
import akkaviz.frontend.components._
import akkaviz.protocol
import akkaviz.protocol._
import org.scalajs.dom.raw.{CloseEvent, ErrorEvent, MessageEvent}
import org.scalajs.dom.{console, document}
import rx._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.scalajs.js.{JSApp, timers}
import scalatags.JsDom.all._

case class FsmTransition(fromStateClass: String, toStateClass: String)

object FrontendApp extends JSApp with Persistence with PrettyJson with ManipulationsUI {

  private[this] val repo = new ActorRepository()

  private def handleDownstream(messageReceived: (Received) => Unit)(messageEvent: MessageEvent): Unit = {
    val bb = TypedArrayBuffer.wrap(messageEvent.data.asInstanceOf[ArrayBuffer])
    val message: ApiServerMessage = protocol.IO.readServer(bb)

    def addActorLink(sender: String, receiver: String, linkType: LinkType): Unit = {
      val actors = Seq(sender, receiver).filter(FrontendUtil.isUserActor)
      repo.addActorsToSeen(actors)
      if (actors.length > 1) {
        // means both are user actor
        graphView.addLink(sender, receiver, linkType)
      }
    }

    message match {
      case ActorSystemCreated(system) =>

      case rcv: Received =>
        messageReceived(rcv)
        addActorLink(rcv.sender, rcv.receiver, GraphView.TellLink)

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

      case Killed(ref) =>
        repo.mutateActor(ref) {
          _.copy(isDead = true)
        }

      case af: ActorFailure =>
        thrownExceptions() = af +: thrownExceptions.now

      case q: Question =>
        asksPanel.receivedQuestion(q)
        q.sender.foreach {
          sender =>
            addActorLink(sender, q.actorRef, GraphView.AskLink)
        }

      case a: Answer =>
        asksPanel.receivedAnswer(a)

      case af: AnswerFailed =>
        asksPanel.receivedAnswerFailed(af)

      case SnapshotAvailable(live, deadActors, rcv) =>
        rcv.foreach {
          case (from, to) => {
            addActorLink(from, to, GraphView.TellLink)
          }
        }

        for {
          (ref, clzMaybe) <- live ++ deadActors
          clz <- clzMaybe
        } repo.mutateActor(ref)(_.copy(className = clz))

        deadActors.keys.foreach {
          repo.mutateActor(_) {
            _.copy(isDead = true)
          }
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

  private[this] val monitoringStatus = Var[MonitoringStatus](UnknownYet)
  private[this] val selectedActors = persistedVar[Set[String]](Set(), "selectedActors")
  private[this] val seenMessages = Var[Set[String]](Set())
  private[this] val selectedMessages = persistedVar[Set[String]](Set(), "selectedMessages")
  private[this] val thrownExceptions = Var[Seq[ActorFailure]](Seq())
  private[this] val showUnconnected = Var[Boolean](false)
  private[this] val actorSelector = new ActorSelector(repo.seenActors, selectedActors, repo.state, thrownExceptions)
  private[this] val messageFilter = new MessageFilter(seenMessages, selectedMessages, selectedActors)
  private[this] val messagesPanel = new MessagesPanel(selectedActors)
  private[this] val asksPanel = new AsksPanel(selectedActors)
  private[this] val monitoringOnOff = new MonitoringOnOff(monitoringStatus)
  private[this] val connectionAlert = new Alert()
  private[this] val unconnectedOnOff = new UnconnectedOnOff(showUnconnected)
  private[this] val replTerminal = new ReplTerminal()
  private[this] val graphView = new GraphView(showUnconnected, actorSelector.toggleActor, ActorStateAsNodeRenderer.render)
  private[this] val maxRetries = 10

  def main(): Unit = {

    repo.seenActors.triggerLater {
      repo.seenActors.now.foreach {
        actor =>
          graphView.addActor(actor, repo.state(actor))
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

        selectedMessages.triggerLater {
          console.log(s"Will send allowedClasses: ${selectedMessages.now.mkString("[", ",", "]")}")
          upstream.send(SetAllowedMessages(selectedMessages.now))
        }

        selectedActors.trigger {
          console.log(s"Will send ObserveActors: ${selectedActors.now.mkString("[", ",", "]")}")
          upstream.send(ObserveActors(selectedActors.now))
        }

        delayMillis.triggerLater {
          import scala.concurrent.duration._
          upstream.send(SetReceiveDelay(delayMillis.now.millis))
        }

        monitoringStatus.triggerLater {
          monitoringStatus.now match {
            case Awaiting(s) =>
              console.log("monitoring status: ", monitoringStatus.now.toString)
              upstream.send(SetEnabled(s.asBoolean))
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

    connectionAlert.attach(document.body)
    actorSelector.attach(document.getElementById("actorselection"))
    messageFilter.attach(document.getElementById("messagefiltering"))
    messagesPanel.attach(document.getElementById("messagelist"))
    asksPanel.attach(document.getElementById("asklist"))
    document.getElementById("receivedelay").appendChild(receiveDelayPanel.render) //FIXME: port to component
    monitoringOnOff.attach(document.getElementById("onoffsettings"))
    unconnectedOnOff.attach(document.getElementById("graphsettings"))
    replTerminal.attach(document.getElementById("repl"))
    graphView.attach(document.getElementById("graphview"))

    DOMGlobalScope.$.material.init()

  }

}