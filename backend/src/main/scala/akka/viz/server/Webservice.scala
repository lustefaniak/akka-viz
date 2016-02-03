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

object ApiMessages {

  import upickle.default._

  def read(str: String): protocol.ApiClientMessage = {
    upickle.default.read[protocol.ApiClientMessage](str)
  }

  def write(msg: protocol.ApiServerMessage): String = {
    upickle.default.write(msg)
  }

}

class Webservice(implicit fm: Materializer, system: ActorSystem) {

  import Directives._

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

    val wsIn = Flow[Message].mapConcat[FilteringRule] {
      case TextMessage.Strict(msg) =>
        val command = ApiMessages.read(msg)
        command match {
          case protocol.SetAllowedMessages(classNames) =>
            system.log.debug(s"Set allowed messages to $classNames")
            List(AllowedClasses(classNames))
          case protocol.SetReceiveDelay(duration) =>
            system.log.debug(s"Setting receive delay to $duration")
            EventSystem.receiveDelay = duration
            Nil
          case other =>
            system.log.error(s"Received unsupported unpickled object via WS: ${other}")
            Nil
        }
      case other =>
        system.log.error(s"Received unsupported Message in WS: ${other}")
        Nil
    }.prepend(Source.single(FilteringRule.Default))
      .expand(identity)(r => (r, r))

    val out = wsIn.zipMat(eventSrc)((_, m) => m)
      .collect {
        case (allowed, r: ReceivedWithId) if allowed(r)        => r
        case (_, other) if !other.isInstanceOf[ReceivedWithId] => other
      }.via(internalToApi)
      .via(eventSerialization)
      .map(TextMessage(_))

    out
  }

  def internalToApi: Flow[BackendEvent, protocol.ApiServerMessage, Any] = Flow[BackendEvent].map {
    case ReceivedWithId(eventId, sender, receiver, message) =>
      //FIXME: decide if content of payload should be added to message
      protocol.Received(eventId, sender.path.toSerializationFormat, receiver.path.toSerializationFormat, message.getClass.getName, Some(MessageSerialization.render(message)))
    case AvailableMessageTypes(types) =>
      protocol.AvailableClasses(types.map(_.getName))
    case Spawned(ref, parent) =>
      protocol.Spawned(ref.path.toSerializationFormat, parent.path.toSerializationFormat)
    case Instantiated(ref, clazz) =>
      protocol.Instantiated(ref.path.toSerializationFormat, clazz.getClass.getName)
    case FSMTransition(ref, currentState, currentData, nextState, nextData) =>
      protocol.FSMTransition(
        ref.path.toSerializationFormat,
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
      protocol.CurrentActorState(ref.path.toSerializationFormat, MessageSerialization.render(actor))
    case MailboxStatus(owner, size) =>
      protocol.MailboxStatus(owner.path.toSerializationFormat, size)
    case ReceiveDelaySet(current) =>
      protocol.ReceiveDelaySet(current)
    case Killed(ref) =>
      protocol.Killed(ref.path.toSerializationFormat)
  }

  def eventSerialization: Flow[protocol.ApiServerMessage, String, Any] = Flow[protocol.ApiServerMessage].map {
    msg => ApiMessages.write(msg)
  }

}
