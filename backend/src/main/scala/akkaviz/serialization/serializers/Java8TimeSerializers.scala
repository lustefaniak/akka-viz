package akkaviz.serialization.serializers

import akkaviz.serialization.{AkkaVizSerializer, SerializationContext}
import upickle.Js
import upickle.Js.Value

class Java8TimeSerializers extends AkkaVizSerializer {
  override def canSerialize(obj: Any): Boolean = {
    obj.getClass.getName.startsWith("java.time")
  }

  override def serialize(obj: Any, context: SerializationContext): Value = {
    Js.Obj(
      "$type" -> Js.Str(obj.getClass.getName),
      "value" -> Js.Str(obj.toString)
    )
  }
}
