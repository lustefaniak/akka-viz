package akka.viz.events

import akka.actor.{ActorPath, ActorRef}

package object internal {

  sealed trait Event

  case class Received(sender: ActorRef, receiver: ActorRef, message: Any) extends Event

  case class Spawned(path: ActorRef, parent: ActorRef) extends Event
}
