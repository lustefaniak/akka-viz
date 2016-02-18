package akkaviz.serialization

import akkaviz.config.Config
import upickle.Js

trait ReflectiveSerialization {

  private val maxSerializationDepth = Config.maxSerializationDepth

  private val shouldInspectObjects = Config.inspectObjects

  @inline
  protected def fieldSelector(inspector: ClassInspector): Set[String] = {
    inspector.allFieldNames
  }

  @inline
  def reflectiveSerialize(obj: Any, context: SerializationContext): Js.Value = {
    if (context.depth() <= maxSerializationDepth) {
      inspect(obj, context)
    } else {
      maxDepthReached
    }
  }

  @inline
  private def inspect(obj: Any, context: SerializationContext): Js.Value = {
    val inspector = CachingClassInspector.of(obj.getClass)
    if (!inspector.isScalaObject || shouldInspectObjects) {
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
  }

  @inline
  private def maxDepthReached: Js.Value = {
    Js.Obj("$error" -> Js.Str(s"Max serialization depth of ${maxSerializationDepth} reached"))
  }

  @inline
  private def shallowModuleSerialization(module: Any): Js.Value = {
    Js.Obj("$object" -> Js.Str(module.getClass.getName.stripSuffix("$")))
  }

}
