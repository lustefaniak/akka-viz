package akka.viz.serialization.serializers

import akka.viz.serialization.{AkkaVizSerializer, SerializationContext}
import upickle.Js

case object JsonAstSerializer extends AkkaVizSerializer {
  override def serialize(obj: Any, context: SerializationContext): Js.Value = {
    obj match {
      case a: Js.Value => a
    }
  }

  override def canSerialize(obj: Any): Boolean = obj match {
    case t: Js.Value => true
    case _           => false
  }
}

