package akka.viz.serialization

import akka.viz.config.Config
import upickle.Js

trait ReflectiveSerialization {

  private val maxSerializationDepth = Config.maxSerializationDepth

  private val shouldInspectObjects = Config.inspectObjects

  def fieldSelector(inspector: ClassInspector): Set[String] = {
    inspector.allFieldNames
  }

  def reflectiveSerialize(obj: Any, context: SerializationContext): Js.Value = {
    if (context.depth() <= maxSerializationDepth) {
      val inspector = CachingClassInspector.of(obj.getClass)
      if (!inspector.isObject || shouldInspectObjects) {
        val fields = inspector.inspect(obj, fieldSelector(inspector))
        Js.Obj(
          Seq("$type" -> Js.Str(obj.getClass.getName))
            ++ fields.toSeq.map {
              case (fieldName, rawValue) =>
                fieldName -> MessageSerialization.serialize(rawValue, context)
            }: _*
        )
      } else {
        shallowModuleSerialization(obj)
      }
    } else {
      maxDepthReached
    }
  }

  protected def maxDepthReached: Js.Value = {
    Js.Obj("$error" -> Js.Str(s"Max serialization depth of ${maxSerializationDepth} reached"))
  }

  protected def shallowModuleSerialization(module: Any): Js.Value = {
    Js.Obj("$object" -> Js.Str(module.getClass.getName.stripSuffix("$")))
  }

}
