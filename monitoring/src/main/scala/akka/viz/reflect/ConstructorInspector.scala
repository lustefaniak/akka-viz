package akka.viz.reflect

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

class ConstructorInspector[T](inspectedInstance: T)(implicit tt: TypeTag[T], ct: ClassTag[T]) {
  lazy val constructorSymbols = {
    tt.tpe.member(termNames.CONSTRUCTOR).asMethod.paramLists.head
  }

  def constructorParameterValues = {
    val m = ru.runtimeMirror(inspectedInstance.getClass.getClassLoader)
    val mirror = m.reflect(inspectedInstance)

    val mSymbols = tt.tpe.members.filter(mSymbol => constructorSymbols.exists(cSymbol => cSymbol.name == mSymbol.name))

    (for {
      sym <- mSymbols
      term = sym.asTerm
    } yield (term, mirror.reflectField(term).get)).toMap

  }


}
