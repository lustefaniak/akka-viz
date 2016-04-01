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
import akkaviz.persistence.{PersistenceSources, ReceivedRecord}
import akkaviz.protocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Webservice(implicit materializer: Materializer, system: ActorSystem)
    extends Directives with SubscriptionSession with ReplSupport with AkkaHttpHelpers with ArchiveSupport
    with FrontendResourcesSupport with ProtocolSerializationSupport with BackendEventsMarshalling {

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
      }.via(backendEventToProtocolFlow)
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

  override def receivedOf(ref: String): Source[ReceivedRecord, _] = PersistenceSources.of(ref)

  override def receivedBetween(ref: String, ref2: String): Source[ReceivedRecord, _] = PersistenceSources.between(ref, ref2)

  override def isArchiveEnabled: Boolean = Config.enableArchive

}
