package akka.viz.events

import akka.actor.{Actor, ActorRef}

import scala.concurrent.duration.Duration

package object types {

  sealed trait InternalEvent

  sealed trait BackendEvent

  case class Received(sender: ActorRef, receiver: ActorRef, message: Any) extends InternalEvent

  case class ReceivedWithId(eventId: Long, sender: ActorRef, receiver: ActorRef, message: Any) extends BackendEvent

  case class Spawned(ref: ActorRef, parent: ActorRef) extends InternalEvent with BackendEvent

  case class MailboxStatus(owner: ActorRef, size: Int) extends InternalEvent with BackendEvent

  case class Instantiated(actorRef: ActorRef, actor: Actor) extends InternalEvent with BackendEvent

  case class AvailableMessageTypes(classes: List[Class[_ <: Any]]) extends BackendEvent

  case class FSMTransition(
    actorRef: ActorRef,
    currentState: Any,
    currentData: Any,
    nextState: Any,
    nextData: Any
  ) extends InternalEvent with BackendEvent

  case class CurrentActorState(actorRef: ActorRef, actor: Actor) extends InternalEvent with BackendEvent

  case class ReceiveDelaySet(duration: Duration) extends InternalEvent with BackendEvent

  case class Killed(actorRef: ActorRef) extends InternalEvent with BackendEvent

  case object ReportingEnabled extends InternalEvent with BackendEvent

  case object ReportingDisabled extends InternalEvent with BackendEvent

}
