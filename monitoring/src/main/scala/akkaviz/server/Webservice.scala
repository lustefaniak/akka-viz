package akkaviz.server

import akka.actor.{ActorRef, ActorSystem, Kill, PoisonPill}
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl._
import akka.stream.{Materializer, OverflowStrategy}
import akkaviz.config.Config
import akkaviz.events._
import akkaviz.events.types._
import akkaviz.protocol
import akkaviz.serialization.MessageSerialization

import scala.collection.breakOut
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Webservice(implicit materializer: Materializer, system: ActorSystem)
    extends Directives with SubscriptionSession with ReplSupport with AkkaHttpHelpers with ArchiveSupport with FrontendResourcesSupport with ProtocolSerializationSupport {

  def route: Flow[HttpRequest, HttpResponse, Any] = encodeResponseWith(Gzip) {
    get {
      path("stream") {
        handleWebSocketMessages(tracingEventsFlow.mapMaterializedValue(EventSystem.subscribe))
      }
    } ~
      archiveRouting ~
      replRouting ~
      frontendResourcesRouting
  }

  def tracingEventsFlow: Flow[Message, Message, ActorRef] = {
    val eventSrc = Source.actorRef[BackendEvent](Config.bufferSize, OverflowStrategy.dropNew)

    val wsIn = Flow[Message]
      .via(websocketMessageToClientMessage)
      .via(handleUserCommand)
      .scan(defaultSettings)(updateSettings)
      .expand(r => Iterator.continually(r))

    val out = wsIn.zipMat(eventSrc)((_, m) => m)
      .collect {
        case (settings, r: BackendEvent) if settings.eventAllowed(r) => r
      }.via(internalToApi)
      .keepAlive(10.seconds, () => protocol.Ping)
      .via(protocolServerMessageToByteString)
      .map(BinaryMessage.Strict(_))

    out
  }

  private[this] val handleUserCommand: Flow[protocol.ApiClientMessage, ChangeSubscriptionSettings, _] = Flow[protocol.ApiClientMessage].mapConcat {
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
      system.log.info(s"Setting EventSystem.setEnabled($isEnabled)")
      EventSystem.setEnabled(isEnabled)
      Nil
    case protocol.RefreshInternalState(actor) =>
      ActorSystems.refreshActorState(actor)
      Nil
    case protocol.PoisonPillActor(actor) =>
      ActorSystems.tell(actor, PoisonPill)
      Nil
    case protocol.KillActor(actor) =>
      ActorSystems.tell(actor, Kill)
      Nil
  }

  @inline
  private[this] implicit val actorRefToString: Function1[ActorRef, String] = Helpers.actorRefToString

  private[this] def internalToApi: Flow[BackendEvent, protocol.ApiServerMessage, Any] = Flow[BackendEvent].map {
    case ReceivedWithId(eventId, sender, receiver, message, handled) =>
      protocol.Received(eventId, sender, receiver, message.getClass.getName, Some(MessageSerialization.render(message)), handled)
    case AvailableMessageTypes(types) =>
      protocol.AvailableClasses(types.map(_.getName))
    case Spawned(ref) =>
      protocol.Spawned(ref)
    case ActorSystemCreated(system) =>
      protocol.ActorSystemCreated(system.name)
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
    case ActorFailure(ref, cause, decision, ts) =>
      protocol.ActorFailure(
        ref,
        cause.toString,
        decision.toString,
        java.time.Instant.ofEpochMilli(ts).toString
      )

    case Question(id, senderOpt, ref, msg) =>
      protocol.Question(
        id,
        senderOpt.map(x => actorRefToString(x)),
        ref,
        MessageSerialization.render(msg)
      )

    case Answer(questionId, msg) =>
      protocol.Answer(questionId, MessageSerialization.render(msg))

    case AnswerFailed(questionId, ex) =>
      protocol.AnswerFailed(questionId, ex.toString)

    case ReportingDisabled =>
      protocol.ReportingDisabled
    case ReportingEnabled =>
      protocol.ReportingEnabled
    case SnapshotAvailable(s) =>
      protocol.SnapshotAvailable(
        s.liveActors.map(ref => ref -> s.classNameFor(ref))(breakOut),
        s.dead.map(ref => ref -> s.classNameFor(ref))(breakOut),
        s.receivedFrom
      )
    case ThroughputMeasurement(ref, msgs, ts) =>
      protocol.ThroughputMeasurement(ref, msgs, tsToIsoTs(ts))
  }

  private[this] def tsToIsoTs(ts: EventTs): String = {
    java.time.Instant.ofEpochMilli(ts).toString
  }

}
