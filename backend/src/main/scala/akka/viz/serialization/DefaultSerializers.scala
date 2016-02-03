package akka.viz.serialization

import upickle.Js
import upickle.Js.Value

import scala.collection.mutable
import scala.reflect.ClassTag

object DefaultSerializers {

  private implicit def fnToAkkaVizSerializer[T](fn: (T) => Js.Value) = {
    new AkkaVizSerializer {
      override def serialize(obj: Any): Value = fn(obj.asInstanceOf[T])

      override def canSerialize(obj: Any): Boolean = {
        //They are already registered, so no need to try them at all
        false
      }
    }
  }

  def mappers: mutable.Map[Class[_], AkkaVizSerializer] = mutable.Map(
    classOf[String] -> ((s: String) => Js.Str(s)),
    classOf[Int] -> ((i: Int) => Js.Num(i)),
    classOf[java.lang.Integer] -> ((i: java.lang.Integer) => Js.Num(i.toInt)),
    classOf[Long] -> ((i: Long) => Js.Num(i)),
    classOf[java.lang.Long] -> ((i: java.lang.Long) => Js.Num(i.toLong)),
    classOf[Double] -> ((i: Double) => Js.Num(i)),
    classOf[java.lang.Double] -> ((i: java.lang.Double) => Js.Num(i)),
    classOf[Float] -> ((i: Float) => Js.Num(i)),
    classOf[java.lang.Float] -> ((i: java.lang.Float) => Js.Num(i.toFloat)),
    classOf[Boolean] -> ((b: Boolean) => if (b) Js.True else Js.False),
    classOf[java.lang.Boolean] -> ((b: java.lang.Boolean) => if (b) Js.True else Js.False)
  )

}
