package akka.viz.events

import akka.actor._

object FilteringRule {
  val Default = IsUserActor
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
  @transient private val allowedClasses = names.map(name => getClass.getClassLoader.loadClass(name)).toSet
  override def apply(event: Received): Boolean = {
    allowedClasses.exists(c => c.isAssignableFrom(event.message.getClass))
  }
}
