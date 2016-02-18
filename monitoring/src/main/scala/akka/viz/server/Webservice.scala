package akka.viz.server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl._
import akka.stream.{Materializer, OverflowStrategy}
import akka.viz.config.Config
import akka.viz.events._
import akka.viz.events.types._
import akka.viz.protocol
import akka.viz.serialization.MessageSerialization

import scala.concurrent.duration._

class Webservice(implicit fm: Materializer, system: ActorSystem) extends Directives with SubscriptionSession {

  def route: Flow[HttpRequest, HttpResponse, Any] = get {
    pathSingleSlash {
      getFromResource("web/index.html")
    } ~
      path("frontend-launcher.js")(getFromResource("frontend-launcher.js")) ~
      path("frontend-fastopt.js")(getFromResource("frontend-fastopt.js")) ~
      path("frontend-jsdeps.js")(getFromResource("frontend-jsdeps.js")) ~
      path("stream") {
        handleWebsocketMessages(tracingEventsFlow.mapMaterializedValue(EventSystem.subscribe))

      }
  } ~
    getFromResourceDirectory("web")

  def tracingEventsFlow: Flow[Message, Message, ActorRef] = {
    val eventSrc = Source.actorRef[BackendEvent](Config.bufferSize, OverflowStrategy.dropNew)

    val wsIn = Flow[Message].mapConcat[ChangeSubscriptionSettings] {
      case TextMessage.Strict(msg) =>
        val command = protocol.IO.readClient(msg)
        command match {
          case protocol.SetAllowedMessages(classNames) =>
            system.log.debug(s"Set allowed messages to $classNames")
            List(SetAllowedClasses(classNames))
          case protocol.ObserveActors(actors) =>
            system.log.debug(s"Set observed actors to $actors")
            List(SetActorEventFilter(actors))
          case protocol.SetReceiveDelay(duration) =>
            system.log.debug(s"Setting receive delay to $duration")
            EventSystem.receiveDelay = duration
            Nil
          case protocol.SetEnabled(isEnabled) =>
            system.log.info(s"Setting EventSystem.setEnabled(${isEnabled})")
            EventSystem.setEnabled(isEnabled)
            Nil
          case other =>
            system.log.error(s"Received unsupported unpickled object via WS: ${other}")
            Nil
        }
      case other =>
        system.log.error(s"Received unsupported Message in WS: ${other}")
        Nil
    }
      .scan(defaultSettings)(updateSettings)
      .expand(identity)(r => (r, r))

    val out = wsIn.zipMat(eventSrc)((_, m) => m)
      .collect {
        case (settings, r: BackendEvent) if settings.eventAllowed(r) => r
      }.via(internalToApi)
      .keepAlive(10.seconds, () => protocol.Ping)
      .via(eventSerialization)
      .map(TextMessage(_))

    out
  }

  @inline
  private implicit val actorRefToString = akka.viz.events.Helpers.actorRefToString _

  def internalToApi: Flow[BackendEvent, protocol.ApiServerMessage, Any] = Flow[BackendEvent].map {
    case ReceivedWithId(eventId, sender, receiver, message) =>
      //FIXME: decide if content of payload should be added to message
      protocol.Received(eventId, sender, receiver, message.getClass.getName, Some(MessageSerialization.render(message)))
    case AvailableMessageTypes(types) =>
      protocol.AvailableClasses(types.map(_.getName))
    case Spawned(ref, parent) =>
      protocol.Spawned(ref, parent)
    case Instantiated(ref, clazz) =>
      protocol.Instantiated(ref, clazz.getClass.getName)
    case FSMTransition(ref, currentState, currentData, nextState, nextData) =>
      protocol.FSMTransition(
        ref,
        currentState = MessageSerialization.render(currentState),
        currentStateClass = currentState.getClass.getName,
        currentData = MessageSerialization.render(currentData),
        currentDataClass = currentData.getClass.getName,
        nextState = MessageSerialization.render(nextState),
        nextStateClass = nextState.getClass.getName,
        nextData = MessageSerialization.render(nextData),
        nextDataClass = nextData.getClass.getName
      )
    case CurrentActorState(ref, actor) =>
      protocol.CurrentActorState(ref, MessageSerialization.render(actor))
    case MailboxStatus(owner, size) =>
      protocol.MailboxStatus(owner, size)
    case ReceiveDelaySet(current) =>
      protocol.ReceiveDelaySet(current)
    case Killed(ref) =>
      protocol.Killed(ref)
    case ActorFailure(ref, cause, decision) =>
      protocol.ActorFailure(
        ref,
        cause.toString,
        decision.toString
      )
    case ReportingDisabled =>
      protocol.ReportingDisabled
    case ReportingEnabled =>
      protocol.ReportingEnabled
    case SnapshotAvailable(s) =>
      protocol.SnapshotAvailable(s.liveActors.toList, s.dead.toList, s.children, s.receivedFrom)
  }

  def eventSerialization: Flow[protocol.ApiServerMessage, String, Any] = Flow[protocol.ApiServerMessage].map {
    msg => protocol.IO.write(msg)
  }

}
