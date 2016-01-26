package akka.viz.events

import akka.actor.ActorRef

package object internal {

  sealed trait Event

  case class Received(sender: ActorRef, receiver: ActorRef, message: Any) extends Event

}
