package akkaviz.events

import akka.actor._

import scala.util.Try

object FilteringRule {

  def isUserActor(actorRef: ActorRef): Boolean = actorRef.path.elements.headOption.contains("user")
}

case class AllowedClasses(names: Set[String]) extends (Any => Boolean) {
  @transient private[this] val allowedClasses = names.flatMap { name =>
    val loaded: Try[Class[_]] = Try(getClass.getClassLoader.loadClass(name))
    loaded.failed.foreach(thr => println(s"class loading failed: $thr"))
    loaded.toOption
  }

  override def apply(payload: Any): Boolean = {
    allowedClasses.exists(c => c.isAssignableFrom(payload.getClass))
  }
}

case class ActorRefFilter(actors: Set[String]) extends (ActorRef => Boolean) {
  @transient private[this] val paths = actors.flatMap {
    path =>
      Try {
        ActorPath.fromString(path)
      }.toOption
  }

  override def apply(v1: ActorRef): Boolean = {
    paths(v1.path)
  }
}