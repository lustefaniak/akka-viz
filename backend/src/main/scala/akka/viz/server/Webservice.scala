package akka.viz.server


import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl._
import akka.stream.{Materializer, OverflowStrategy}
import akka.viz.config.Config
import akka.viz.events._
import akka.viz.{MessageSerialization, protocol}

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

  def route: Flow[HttpRequest, HttpResponse, Any] = {
    get {
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~
        path("frontend-launcher.js")(getFromResource("frontend-launcher.js")) ~
        path("frontend-fastopt.js")(getFromResource("frontend-fastopt.js")) ~
        path("frontend-jsdeps.js")(getFromResource("frontend-jsdeps.js")) ~
        path("stream") {
          handleWebsocketMessages(tracingEventsFlow)

        }
    } ~
      getFromResourceDirectory("web")
  }

  def tracingEventsFlow: Flow[Message, Message, Any] = {
    val in = Flow[Message].to(Sink.foreach {
      case TextMessage.Strict(msg) =>
        val command = ApiMessages.read(msg)
        command match {
          case protocol.SetAllowedMessages(classNames) =>
            EventSystem.updateFilter(AllowedClasses(classNames))
        }
      case other =>
        system.log.error(s"Received unsupported Message in WS: ${other}")
    })

    val out = Source.actorRef[Event](Config.bufferSize, OverflowStrategy.dropNew)
      .via(internalToApi)
      .via(eventSerialization)
      .map(TextMessage(_))
      .mapMaterializedValue(EventSystem.subscribe(_))

    Flow.fromSinkAndSource(in, out)
  }

  def internalToApi: Flow[Event, protocol.ApiServerMessage, Any] = Flow[Event].map {
    case Received(sender, receiver, message) =>
      protocol.Received(sender.path.toSerializationFormat, receiver.path.toSerializationFormat, MessageSerialization.serialize(message))
    case AvailableMessageTypes(types) =>
      protocol.AvailableClasses(types.map(_.getCanonicalName))

  }

  def eventSerialization: Flow[protocol.ApiServerMessage, String, Any] = Flow[protocol.ApiServerMessage].map {
    msg => ApiMessages.write(msg)
  }

}
