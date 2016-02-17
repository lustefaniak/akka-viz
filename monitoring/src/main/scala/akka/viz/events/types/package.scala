package akka.viz.events

import akka.actor.{SupervisorStrategy, Actor, ActorRef}
import akka.dispatch.ControlMessage

import scala.concurrent.duration.Duration

package object types {

  sealed trait FilteredActorEvent {
    def actorRef: ActorRef
  }

  sealed trait InternalEvent

  sealed trait BackendEvent

  sealed trait EventPublisherControlEvent extends ControlMessage { this: InternalEvent => }

  case class Received(sender: ActorRef, actorRef: ActorRef, message: Any) extends InternalEvent with FilteredActorEvent

  case class ReceivedWithId(eventId: Long, sender: ActorRef, actorRef: ActorRef, message: Any) extends BackendEvent with FilteredActorEvent

  case class Spawned(actorRef: ActorRef, parent: ActorRef) extends InternalEvent with BackendEvent

  case class MailboxStatus(actorRef: ActorRef, size: Int) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class Instantiated(actorRef: ActorRef, actor: Actor) extends InternalEvent with BackendEvent

  case class AvailableMessageTypes(classes: List[Class[_ <: Any]]) extends BackendEvent

  case class FSMTransition(
    actorRef: ActorRef,
    currentState: Any,
    currentData: Any,
    nextState: Any,
    nextData: Any
  ) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class CurrentActorState(actorRef: ActorRef, actor: Actor) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class ReceiveDelaySet(duration: Duration) extends InternalEvent with BackendEvent

  case class Killed(actorRef: ActorRef) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class ActorFailure(
    actorRef: ActorRef,
    cause: Throwable,
    decision: SupervisorStrategy.Directive
  ) extends InternalEvent with BackendEvent

  case object ReportingEnabled extends InternalEvent with BackendEvent with EventPublisherControlEvent

  case object ReportingDisabled extends InternalEvent with BackendEvent with EventPublisherControlEvent

  case class SnapshotAvailable(snapshot: LightSnapshot) extends BackendEvent

}
