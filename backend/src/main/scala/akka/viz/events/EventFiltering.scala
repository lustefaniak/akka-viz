package akka.viz.events

import akka.actor.Status.Success
import akka.actor._

import scala.util.{Failure, Try}

object FilteringRule {
  val Default: FilteringRule = IsUserActor
}

sealed trait FilteringRule {
  def apply(received: Received): Boolean
}

case object IsUserActor extends FilteringRule {
  private def isUserActor(actorRef: ActorRef): Boolean = actorRef.path.elements.headOption.contains("user")

  override def apply(event: Received) = {
    isUserActor(event.sender) || isUserActor(event.receiver)
  }
}

case object AllowAll extends FilteringRule { override def apply(event: Received) = true }

case class AllowedClasses(names: List[String]) extends FilteringRule {
  @transient private val allowedClasses = names.flatMap { name =>
    val loaded: Try[Class[_]] = Try(getClass.getClassLoader.loadClass(name))
    loaded.failed.foreach(thr => println(s"class loading failed: $thr"))
    loaded.toOption.toIterable
  }


  override def apply(event: Received): Boolean = {
    allowedClasses.exists(c => c.isAssignableFrom(event.message.getClass))
  }
}
