package akka.viz.serialization

import org.clapper.classutil.ClassFinder
import upickle.Js
import upickle.json.FastRenderer

trait AkkaVizSerializer {
  def canSerialize(obj: Any): Boolean

  def serialize(obj: Any): Js.Value
}

object MessageSerialization {

  private val reflectiveSerializer = new AkkaVizSerializer {
    override def canSerialize(obj: Any): Boolean = {
      //don't allow autoload just in case
      false
    }

    def serialize(obj: Any): Js.Value = {
      val inspector = CachingClassInspector.of(obj.getClass)
      val fields = inspector.inspect(obj)
      Js.Obj(
        Seq("$type" -> Js.Str(obj.getClass.getName))
          ++ fields.toSeq.map {
          case (fieldName, rawValue) =>
            fieldName -> getSerializerFor(rawValue).serialize(rawValue)
        }: _*)

    }
  }

  private val rm = scala.reflect.runtime.currentMirror

  private lazy val serializers: List[AkkaVizSerializer] = {
    val finder = ClassFinder()
    val classes = finder.getClasses.filter(_.isConcrete).filter(_.implements("akka.viz.serialization.AkkaVizSerializer"))

    classes.flatMap {
      cls =>
        val clazz = Class.forName(cls.name)
        val classSymbol = rm.classSymbol(clazz)
        if (classSymbol.isModule) {
          Some(rm.reflectModule(classSymbol.asModule).instance.asInstanceOf[AkkaVizSerializer])
        } else {
          val constructors = classSymbol.toType.members.filter(_.isConstructor).map(_.asMethod)
          val constructor = constructors.filter(_.isPublic).filter(_.paramLists.exists(_.size == 0)).headOption
          constructor.map {
            constructor =>
              rm.reflectClass(classSymbol).reflectConstructor(constructor).apply().asInstanceOf[AkkaVizSerializer]
          }
        }
    }.toList
  }

  private lazy val mappers = DefaultSerializers.mappers

  def getSerializerFor(obj: Any): AkkaVizSerializer = {
    def findSerializerForObject(obj: Any): AkkaVizSerializer = {
      serializers.find {
        ser =>
          ser.canSerialize(obj)
      }.getOrElse(reflectiveSerializer)
    }
    mappers.getOrElseUpdate(obj.getClass, findSerializerForObject(obj))
  }

  def serializeToString(message: Any): String = {
    def unableToSerialize(t: Throwable): String = {
      println(s"Unable to serialize '${message}'")
      println(t.getMessage)
      s"{'error':'Failed to serialize: ${t.getMessage}'}"
    }

    try {
      val serialized = serialize(message)
      FastRenderer.render(serialized)
    } catch {
      case t: Throwable => unableToSerialize(t)
    }
  }

  def serialize(message: Any): Js.Value = {
    getSerializerFor(message).serialize(message)
  }


}
