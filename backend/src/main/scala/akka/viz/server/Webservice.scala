package akka.viz.server


import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.stream.{Materializer, OverflowStrategy}
import akka.viz.MessageSerialization
import akka.viz.config.Config
import akka.viz.events.EventSystem.Subscribe
import akka.viz.events.{Received, Event, EventSystem}
import upickle.Js


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

  private implicit val actorRefWriter = upickle.default.Writer[ActorRef] {
    case actorRef => Js.Str(actorRef.path.toSerializationFormat)
  }

  def tracingEventsFlow: Flow[Message, Message, Any] = {
    val in = Flow[Message].to(Sink.ignore)
    val out = Source.actorRef[Event](Config.bufferSize, OverflowStrategy.dropNew)
      .via(eventSerialization)
      .via(jsonPrinter)
      .map(TextMessage(_))
      .mapMaterializedValue(EventSystem.subscribe(_))

    Flow.fromSinkAndSource(in, out)
  }

  def eventSerialization: Flow[Event, Js.Value, Any] = Flow[Event].map {
    case Received(sender, receiver, message) =>
      Js.Obj(
        "sender" -> Js.Str(sender.path.toSerializationFormat),
        "receiver" -> Js.Str(receiver.path.toSerializationFormat),
        "message" -> MessageSerialization.serialize(message)
      )
  }

  def jsonPrinter: Flow[Js.Value, String, Any] = Flow[Js.Value].map {
    case json =>
      upickle.json.write(json)
  }

}
