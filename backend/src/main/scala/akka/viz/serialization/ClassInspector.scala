package akka.viz.serialization

import scala.collection.mutable


sealed trait FieldType

case object Val extends FieldType

case object Var extends FieldType

case class ClassField(name: String, fullName: String, fieldType: FieldType, resultClass: Class[_], signature: String)


trait Inspector[T] {
  def fields: Seq[ClassField]

  def allFieldNames = fields.map(_.name).toSet

  def inspect(obj: T, fields: Set[String] = allFieldNames): Map[String, Any]

  override def toString: String = {
    s"Inspector(" + fields.map(f => s"${f.name}:${f.signature}").mkString(",") + ")"
  }
}


object ClassInspector {
  private val rm = scala.reflect.runtime.currentMirror

  def of[T](clazz: Class[T]): Inspector[T] = {
    val t = rm.classSymbol(clazz).toType

    val reflectedFields = t.members.filter(_.isTerm).map(_.asTerm).filter(t => t.isVal || t.isVar).map {
      s =>
        val fn = s.fullName
        val name = fn.split('.').last
        ClassField(
          name,
          fn,
          if (s.isVal) Val else Var,
          rm.runtimeClass(s.typeSignature),
          s.typeSignature.toString
        )
    }
    new Inspector[T] {
      private val cls: Class[T] = clazz
      private val f = reflectedFields.toSeq

      override def inspect(obj: T, fieldNames: Set[String] = allFieldNames): Map[String, Any] = {
        val result = mutable.Map[String, Any]()
        fieldNames.foreach {
          fieldName =>
            val field = cls.getDeclaredField(fieldName)
            field.setAccessible(true)
            result += (fieldName -> field.get(obj))
        }

        result.toMap
      }

      override def fields: Seq[ClassField] = f
    }
  }


}
