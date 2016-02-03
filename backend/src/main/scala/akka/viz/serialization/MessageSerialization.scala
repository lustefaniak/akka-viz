package akka.viz.serialization

import upickle.Js
import upickle.json.FastRenderer

object MessageSerialization extends SerializerFinder with ReflectiveSerialization {

  def render(message: Any): String = {
    message match {
      case json: Js.Value => FastRenderer.render(json)
      case other          => FastRenderer.render(serialize(other))
    }
  }

  def serialize(message: Any): Js.Value = {
    def unableToSerialize(t: Throwable): Js.Value = {
      Js.Obj("error" -> Js.Str("Failed to serialize: " + t.getMessage))
    }
    try {
      getSerializerFor(message).serialize(message)
    } catch {
      case t: Throwable => unableToSerialize(t)
    }
  }

  private val serializers: List[AkkaVizSerializer] = findSerializers

  private val mappers = DefaultSerializers.mappers

  private def getSerializerFor(obj: Any): AkkaVizSerializer = {
    def findSerializerForObject: AkkaVizSerializer = {
      serializers.find(_.canSerialize(obj)).getOrElse {
        println(s"WARNING: There is no serializer for ${obj.getClass.getName}, consider implementing AkkaVizSerializer")
        reflectiveSerializer
      }
    }
    mappers.getOrElseUpdate(obj.getClass, findSerializerForObject)
  }

  private val reflectiveSerializer = new AkkaVizSerializer {
    override def canSerialize(obj: Any): Boolean = {
      false
    }

    def serialize(obj: Any): Js.Value = {
      reflectiveSerialize(obj)
    }
  }

}
