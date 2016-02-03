package akka.viz.events

import akka.actor.{Actor, ActorRef}

package object backend {

  sealed trait Event

  case class Received(eventId: Long, sender: ActorRef, receiver: ActorRef, message: Any) extends Event

  case class Spawned(eventId: Long, ref: ActorRef, parent: ActorRef) extends Event

  case class MailboxStatus(eventId: Long, owner: ActorRef, size: Int) extends Event

  case class AvailableMessageTypes(classes: List[Class[_ <: Any]]) extends Event

  case class Instantiated(eventId: Long, actorRef: ActorRef, clazz: Class[_ <: Any]) extends Event

  case class FSMTransition(eventId: Long, actorRef: ActorRef, currentState: Any, currentData: Any, nextState: Any, nextData: Any) extends Event

  case class CurrentActorState(eventId: Long, actorRef: ActorRef, actor: Actor) extends Event

}
