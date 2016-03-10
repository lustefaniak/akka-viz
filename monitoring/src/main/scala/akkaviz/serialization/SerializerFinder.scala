package akkaviz.serialization

import org.clapper.classutil.ClassFinder

trait SerializerFinder {
  private[this] val rm = scala.reflect.runtime.currentMirror

  def findSerializers: List[AkkaVizSerializer] = {
    val finder = ClassFinder()
    val classes = finder.getClasses.filter(_.isConcrete).filter(_.implements(classOf[AkkaVizSerializer].getName))

    classes.flatMap {
      cls =>
        val clazz = Class.forName(cls.name)
        val classSymbol = rm.classSymbol(clazz)
        if (classSymbol.isModule) {
          Some(rm.reflectModule(classSymbol.asModule).instance.asInstanceOf[AkkaVizSerializer])
        } else {
          val constructors = classSymbol.toType.members.filter(_.isConstructor).map(_.asMethod)
          val constructorMaybe = constructors.filter(_.isPublic).filter(_.paramLists.exists(_.size == 0)).headOption
          constructorMaybe.map {
            constructor =>
              rm.reflectClass(classSymbol).reflectConstructor(constructor).apply().asInstanceOf[AkkaVizSerializer]
          }
        }
    }.toList
  }

}
