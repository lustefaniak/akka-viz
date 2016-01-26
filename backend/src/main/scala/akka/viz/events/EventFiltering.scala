package akka.viz.events

import akka.actor._

import scala.util.Try

object FilteringRule {

  def isUserActor(actorRef: ActorRef): Boolean = actorRef.path.elements.headOption.contains("user")

  val Default: FilteringRule = AllowAll
}

trait FilteringRule extends Function1[backend.Received, Boolean]

case object AllowAll extends FilteringRule {
  override def apply(event: backend.Received) = true
}

case class AllowedClasses(names: List[String]) extends FilteringRule {
  @transient private val allowedClasses = names.flatMap { name =>
    val loaded: Try[Class[_]] = Try(getClass.getClassLoader.loadClass(name))
    loaded.failed.foreach(thr => println(s"class loading failed: $thr"))
    loaded.toOption
  }

  override def apply(event: backend.Received): Boolean = {
    allowedClasses.exists(c => c.isAssignableFrom(event.message.getClass))
  }
}
