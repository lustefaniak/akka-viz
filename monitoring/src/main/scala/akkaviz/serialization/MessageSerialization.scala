package akkaviz.serialization

import upickle.Js
import upickle.json.FastRenderer

case class SerializationContextImpl(depth: Int = 0) extends SerializationContext

object MessageSerialization extends SerializerFinder with ReflectiveSerialization {

  def preload() = {
    serializers.size
  }

  def render(message: Any): String = {
    message match {
      case json: Js.Value => FastRenderer.render(json)
      case other          => FastRenderer.render(serialize(other, newSerializationContext))
    }
  }

  private[this] def newSerializationContext: SerializationContext = SerializationContextImpl()

  def serialize(message: Any, serializationContext: SerializationContext): Js.Value = {
    def unableToSerialize(t: Throwable): Js.Value = {
      Js.Obj("error" -> Js.Str(s"Failed to serialize: ${t.getMessage} (${t.getClass.getName})"))
    }
    try {
      if (message == null) {
        Js.Null
      } else {
        getSerializerFor(message).serialize(message, SerializationContextImpl(serializationContext.depth + 1))
      }
    } catch {
      case t: Throwable => unableToSerialize(t)
    }
  }

  private[this] lazy val serializers: List[AkkaVizSerializer] = findSerializers

  private[this] lazy val mappers = DefaultSerializers.mappers

  private[this] def getSerializerFor(obj: Any): AkkaVizSerializer = {
    def findSerializerForObject: AkkaVizSerializer = {
      serializers.find(_.canSerialize(obj)).getOrElse {
        println(s"WARNING: There is no serializer for ${obj.getClass.getName}, consider implementing AkkaVizSerializer")
        reflectiveSerializer
      }
    }
    mappers.getOrElseUpdate(obj.getClass, findSerializerForObject)
  }

  private[this] val reflectiveSerializer = new AkkaVizSerializer {
    override def canSerialize(obj: Any): Boolean = {
      false
    }

    def serialize(obj: Any, context: SerializationContext): Js.Value = {
      reflectiveSerialize(obj, context)
    }
  }

}
