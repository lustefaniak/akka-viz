package akka.viz.server


import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl._
import akka.stream.{FlowShape, Materializer, OverflowStrategy}
import akka.viz.config.Config
import akka.viz.events._
import akka.viz.protocol
import akka.viz.events.backend._
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
    val eventSrc = Source.actorRef[Event](Config.bufferSize, OverflowStrategy.dropNew)

    val wsIn = Flow[Message].mapConcat[FilteringRule] {
      case TextMessage.Strict(msg) =>
        val command = ApiMessages.read(msg)
        command match {
          case protocol.SetAllowedMessages(classNames) =>
            system.log.debug(s"Set allowed messages to $classNames")
            List(AllowedClasses(classNames))
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
        case (allowed, r: Received) if allowed(r) => r
        case (_, other) if !other.isInstanceOf[Received] => other
      }.via(internalToApi)
      .via(eventSerialization)
      .map(TextMessage(_))

    out
  }

  def internalToApi: Flow[backend.Event, protocol.ApiServerMessage, Any] = Flow[Event].map {
    case Received(eventId, sender, receiver, message) =>
      //FIXME: decide if content of payload should be added to message
      protocol.Received(eventId, sender.path.toSerializationFormat, receiver.path.toSerializationFormat, message.getClass.getCanonicalName, Some(MessageSerialization.serialize(message)))
    case AvailableMessageTypes(types) =>
      protocol.AvailableClasses(types.map(_.getCanonicalName))
    case Spawned(id, ref, parent) =>
      protocol.Spawned(id, ref.path.toSerializationFormat, parent.path.toSerializationFormat)
    case Instantiated(id, ref, clazz) =>
      protocol.Instantiated(id, ref.path.toSerializationFormat, clazz.getCanonicalName)
    case FSMTransition(id, ref, currentState, currentData, nextState, nextData) =>
      protocol.FSMTransition(
        id,
        ref.path.toSerializationFormat,
        currentState = MessageSerialization.serialize(currentState),
        currentStateClass = currentState.getClass.getCanonicalName,
        currentData = MessageSerialization.serialize(currentData),
        currentDataClass = currentData.getClass.getCanonicalName,
        nextState = MessageSerialization.serialize(nextState),
        nextStateClass = nextState.getClass.getCanonicalName,
        nextData = MessageSerialization.serialize(nextData),
        nextDataClass = nextData.getClass.getCanonicalName
      )
    case MailboxStatus(id, owner, size) =>
      protocol.MailboxStatus(id, owner.path.toSerializationFormat, size)
  }

  def eventSerialization: Flow[protocol.ApiServerMessage, String, Any] = Flow[protocol.ApiServerMessage].map {
    msg => ApiMessages.write(msg)
  }

}
