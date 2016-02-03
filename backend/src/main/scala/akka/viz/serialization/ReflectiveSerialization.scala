package akka.viz.serialization

import upickle.Js

trait ReflectiveSerialization {

  def reflectiveSerialize(obj: Any): Js.Value = {
    val inspector = CachingClassInspector.of(obj.getClass)
    val fields = inspector.inspect(obj)
    Js.Obj(
      Seq("$type" -> Js.Str(obj.getClass.getName))
        ++ fields.toSeq.map {
        case (fieldName, rawValue) =>
          fieldName -> MessageSerialization.serialize(rawValue)
      }: _*)
  }

}
