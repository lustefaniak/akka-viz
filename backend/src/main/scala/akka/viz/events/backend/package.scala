package akka.viz.events

import akka.actor.ActorRef

package object backend {

  sealed trait Event

  case class Received(eventId: Long, sender: ActorRef, receiver: ActorRef, message: Any) extends Event

  case class AvailableMessageTypes(classes: List[Class[_ <: Any]]) extends Event

}
