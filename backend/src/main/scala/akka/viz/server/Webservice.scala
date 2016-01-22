package akka.viz.server


import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl._
import akka.stream.stage._
import akka.stream.{Materializer, OverflowStrategy}
import akka.viz.MessageSerialization
import akka.viz.config.Config
import akka.viz.events.EventSystem.Subscribe
import akka.viz.events._
import spray.json.JsonReader
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
    val in = Flow[Message].to(Sink.foreach {
      case TextMessage.Strict(msg) =>
        import spray.json._
        import DefaultJsonProtocol._
        val jsObj = msg.parseJson.asJsObject // todo after sjs migration: use pickler or something
        val filter = jsObj.fields.get("allowedClasses").map(_.convertTo[List[String]]).map(AllowedClasses)

        filter.foreach { f =>
          EventSystem.updateFilter(f)
        }
      case other =>
        println(other)
    })

    val out = Source.actorRef[Event](Config.bufferSize, OverflowStrategy.dropNew)
      .via(eventSerialization)
      .via(Flow[Js.Value].map {
        i =>
          println(i)
          i
      })
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
    case AvailableMessageTypes(classes) =>
      Js.Obj(
        "availableClasses" -> Js.Arr(classes.map(cls => Js.Str(cls.getName)) :_* )
      )
  }

  def jsonPrinter: Flow[Js.Value, String, Any] = Flow[Js.Value].map {
    case json =>
      upickle.json.write(json)
  }

}
