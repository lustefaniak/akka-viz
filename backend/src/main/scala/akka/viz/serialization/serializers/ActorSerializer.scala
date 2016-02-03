package akka.viz.serialization.serializers

import akka.actor.Actor
import akka.viz.serialization.{CachingClassInspector, AkkaVizSerializer, MessageSerialization}
import upickle.Js

case object ActorSerializer extends AkkaVizSerializer {

  private val actorDefaultFields = CachingClassInspector.of(classOf[Actor]).fields.map(_.name).toSet

  override def serialize(obj: Any): Js.Value = {

    val inspector = CachingClassInspector.of(obj.getClass)
    val customFields = inspector.fields.map(_.name).toSet

    val values = inspector.inspect(obj, customFields -- actorDefaultFields)

    Js.Obj(
      Seq("$type" -> Js.Str(obj.getClass.getName))
        ++ values.toSeq.map {
        case (fieldName, rawValue) =>
          fieldName -> MessageSerialization.serialize(rawValue)
      }: _*)

  }

  override def canSerialize(obj: Any): Boolean = obj match {
    case t: Actor => true
    case _ => false
  }
}


