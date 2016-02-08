package akka.viz.events

import akka.actor.{Actor, ActorRef}

import scala.concurrent.duration.Duration

package object types {

  sealed trait InternalEvent

  sealed trait BackendEvent

  sealed trait ActorEvent {
    def actorRef: ActorRef
  }

  case class Received(sender: ActorRef, actorRef: ActorRef, message: Any) extends InternalEvent with ActorEvent

  case class ReceivedWithId(eventId: Long, sender: ActorRef, actorRef: ActorRef, message: Any) extends BackendEvent with ActorEvent

  case class Spawned(actorRef: ActorRef, parent: ActorRef) extends InternalEvent with BackendEvent with ActorEvent

  case class MailboxStatus(actorRef: ActorRef, size: Int) extends InternalEvent with BackendEvent with ActorEvent

  case class Instantiated(actorRef: ActorRef, actor: Actor) extends InternalEvent with BackendEvent with ActorEvent

  case class AvailableMessageTypes(classes: List[Class[_ <: Any]]) extends BackendEvent

  case class FSMTransition(
    actorRef: ActorRef,
    currentState: Any,
    currentData: Any,
    nextState: Any,
    nextData: Any
  ) extends InternalEvent with BackendEvent with ActorEvent

  case class CurrentActorState(actorRef: ActorRef, actor: Actor) extends InternalEvent with BackendEvent with ActorEvent

  case class ReceiveDelaySet(duration: Duration) extends InternalEvent with BackendEvent

  case class Killed(actorRef: ActorRef) extends InternalEvent with BackendEvent with ActorEvent

  case object ReportingEnabled extends InternalEvent with BackendEvent

  case object ReportingDisabled extends InternalEvent with BackendEvent

  case class SnapshotAvailable(snapshot: LightSnapshot) extends BackendEvent

}
