package akkaviz.serialization.serializers

import akkaviz.serialization.{AkkaVizSerializer, SerializationContext}
import upickle.Js

case object ThrowableSerializer extends AkkaVizSerializer {
  override def canSerialize(obj: scala.Any): Boolean = obj match {
    case t: Throwable => true
    case _            => false
  }

  override def serialize(obj: scala.Any, context: SerializationContext): Js.Value = {
    obj match {
      case t: Throwable => Js.Obj(
        "$type" -> Js.Str(t.getClass.getName),
        "message" -> Js.Str(t.getMessage)
      )
    }
  }
}
