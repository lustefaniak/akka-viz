package akka.viz.events

import akka.actor.{Actor, ActorRef}

package object internal {

  sealed trait Event

  case class Received(sender: ActorRef, receiver: ActorRef, message: Any) extends Event

  case class Spawned(ref: ActorRef, parent: ActorRef) extends Event

  case class MailBoxStatus(owner: ActorRef, size: Int) extends Event

  case class Instantiated(actorRef: ActorRef, actor: Actor) extends Event

  case class FSMTransition(actorRef: ActorRef, currentState: Any, currentData:Any, nextState:Any, nextData: Any) extends Event

}
