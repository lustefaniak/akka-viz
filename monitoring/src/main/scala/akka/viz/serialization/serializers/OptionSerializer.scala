package akka.viz.serialization.serializers

import akka.viz.serialization.{AkkaVizSerializer, MessageSerialization, SerializationContext}
import upickle.Js

case object OptionSerializer extends AkkaVizSerializer {
  override def serialize(obj: Any, context: SerializationContext): Js.Value = {
    obj match {
      case Some(x) => MessageSerialization.serialize(x, context)
      case None    => Js.Null
    }
  }

  override def canSerialize(obj: Any): Boolean = obj match {
    case t: Option[_] => true
    case _            => false
  }
}

