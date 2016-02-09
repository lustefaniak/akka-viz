package akka.viz.serialization.serializers

import akka.actor.ActorRef
import akka.viz.serialization.{AkkaVizSerializer, SerializationContext}
import upickle.Js

case object ActorRefSerializer extends AkkaVizSerializer {
  override def serialize(obj: Any, context: SerializationContext): Js.Value = {
    obj match {
      case a: ActorRef =>
        Js.Obj(
          "$type" -> Js.Str(classOf[ActorRef].getName),
          "path" -> Js.Str(a.path.toSerializationFormat)
        )
    }
  }

  override def canSerialize(obj: Any): Boolean = obj match {
    case t: ActorRef => true
    case _           => false
  }
}

