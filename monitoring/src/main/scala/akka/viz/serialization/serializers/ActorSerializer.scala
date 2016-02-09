package akka.viz.serialization.serializers

import akka.actor.Actor
import akka.viz.serialization._
import upickle.Js

case object ActorSerializer extends AkkaVizSerializer with ReflectiveSerialization {

  private val actorDefaultFields = CachingClassInspector.of(classOf[Actor]).fields.map(_.name).toSet

  override def fieldSelector(inspector: ClassInspector): Set[String] = {
    inspector.allFieldNames -- actorDefaultFields
  }

  override def serialize(obj: Any, context: SerializationContext): Js.Value = {
    reflectiveSerialize(obj, context)
  }

  override def canSerialize(obj: Any): Boolean = obj match {
    case t: Actor => true
    case _        => false
  }
}

