package akkaviz.events

import akka.actor.{Actor, ActorRef, ActorSystem, SupervisorStrategy}
import akka.dispatch.ControlMessage

import scala.concurrent.duration.FiniteDuration

package object types {

  type EventTs = Long

  sealed trait FilteredActorEvent {
    def actorRef: ActorRef
  }

  sealed trait InternalEvent

  sealed trait BackendEvent

  sealed trait EventPublisherControlEvent extends ControlMessage {
    this: InternalEvent =>
  }

  sealed trait TimestampedEvent {
    def timestamp: EventTs
  }

  case class Received(sender: ActorRef, actorRef: ActorRef, message: Any, handled: Boolean) extends InternalEvent with FilteredActorEvent

  case class ReceivedWithId(eventId: Long, sender: ActorRef, actorRef: ActorRef, message: Any, handled: Boolean) extends BackendEvent with FilteredActorEvent

  case class Spawned(actorRef: ActorRef) extends InternalEvent with BackendEvent

  case class ActorSystemCreated(system: ActorSystem) extends InternalEvent with BackendEvent

  case class MailboxStatus(actorRef: ActorRef, size: Int) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class Instantiated(actorRef: ActorRef, actor: Actor) extends InternalEvent with BackendEvent

  case class AvailableMessageTypes(classes: Set[Class[_ <: Any]]) extends BackendEvent

  case class FSMTransition(
    actorRef: ActorRef,
    currentState: Any,
    currentData: Any,
    nextState: Any,
    nextData: Any
  ) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class CurrentActorState(actorRef: ActorRef, actor: Actor) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class ReceiveDelaySet(duration: FiniteDuration) extends InternalEvent with BackendEvent

  case class Killed(actorRef: ActorRef) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class Restarted(actorRef: ActorRef) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class ActorFailure(
    actorRef: ActorRef,
    cause: Throwable,
    decision: SupervisorStrategy.Directive,
    timestamp: EventTs = System.currentTimeMillis()
  ) extends InternalEvent with BackendEvent with TimestampedEvent with FilteredActorEvent

  case class Question(
    id: Long,
    sender: Option[ActorRef],
    actorRef: ActorRef,
    message: Any
  ) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class Answer(questionId: Long, message: Any) extends InternalEvent with BackendEvent

  case class AnswerFailed(questionId: Long, ex: Throwable) extends InternalEvent with BackendEvent

  case object ReportingEnabled extends InternalEvent with BackendEvent with EventPublisherControlEvent

  case object ReportingDisabled extends InternalEvent with BackendEvent with EventPublisherControlEvent

  case class SnapshotAvailable(snapshot: LightSnapshot) extends BackendEvent

  case class ThroughputMeasurement(
    actorRef: ActorRef,
    msgsPerSecond: Double,
    timestamp: Long = System.currentTimeMillis()
  ) extends InternalEvent with BackendEvent with FilteredActorEvent

}
