package akka.viz.serialization

import org.clapper.classutil.ClassFinder
import upickle.Js
import upickle.json.FastRenderer

import scala.collection.mutable
import scala.util.{Success, Try}

trait AkkaVizSerializer {
  def canSerialize(obj: Any): Boolean

  def serialize(obj: Any): Js.Value
}

object MessageSerialization {

  private val placeholderSerializer = new AkkaVizSerializer {
    override def serialize(obj: Any): Js.Value = Js.Obj(
      "$type" -> Js.Str(obj.getClass.getCanonicalName)
    )

    override def canSerialize(obj: Any): Boolean = true
  }

  private val rm = scala.reflect.runtime.currentMirror

  private val serializers: List[AkkaVizSerializer] = {
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

  private val mappers = DefaultSerializers.mappers

  private def findSerializerForObject(obj: Any): AkkaVizSerializer = {
    serializers.find {
      _.canSerialize(obj)
    }.getOrElse(placeholderSerializer)
  }

  def getSerializerFor(obj: Any): AkkaVizSerializer = {
    mappers.getOrElseUpdate(obj.getClass, findSerializerForObject(obj))
  }

  def serialize(message: Any): String = {
    Try {
      val serialized = getSerializerFor(message).serialize(message)
      FastRenderer.render(serialized)
    }.recoverWith {
      case t: Throwable =>
        println(s"Unable to serialize '${message}'")
        Success(s"{'error':'Failed to serialize: ${t.getMessage}'}")
    }.get
  }

}
