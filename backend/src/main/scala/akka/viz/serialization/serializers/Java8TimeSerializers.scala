package akka.viz.serialization.serializers

import java.time.{LocalDateTime, LocalTime, LocalDate}

import akka.viz.serialization.AkkaVizSerializer
import upickle.Js
import upickle.Js.Value

class Java8TimeSerializers extends AkkaVizSerializer {
  override def canSerialize(obj: scala.Any): Boolean = {
    obj.getClass.getName.startsWith("java.time")
  }

  override def serialize(obj: scala.Any): Value = {
    Js.Obj(
      "$type" -> Js.Str(obj.getClass.getName),
      "value" -> Js.Str(obj.toString)
    )
  }
}
