package akkaviz.events

import scala.concurrent.duration.Duration

package object types {

  case class EventActorRef(path: String) extends AnyVal {
    def isUserActor: Boolean = {
      path.contains("user")
    }
  }

  sealed trait FilteredActorEvent {
    def actorRef: EventActorRef
  }

  sealed trait InternalEvent

  sealed trait BackendEvent

  case class Received(sender: EventActorRef, actorRef: EventActorRef, message: Any) extends InternalEvent with FilteredActorEvent

  case class ReceivedWithId(eventId: Long, sender: EventActorRef, actorRef: EventActorRef, message: Any) extends BackendEvent with FilteredActorEvent

  case class Spawned(actorRef: EventActorRef, parent: EventActorRef) extends InternalEvent with BackendEvent

  case class MailboxStatus(actorRef: EventActorRef, size: Int) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class Instantiated(actorRef: EventActorRef, actor: Any) extends InternalEvent with BackendEvent

  case class AvailableMessageTypes(classes: List[Class[_ <: Any]]) extends BackendEvent

  case class FSMTransition(
    actorRef: EventActorRef,
    currentState: Any,
    currentData: Any,
    nextState: Any,
    nextData: Any
  ) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class CurrentActorState(actorRef: EventActorRef, actor: Any) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class ReceiveDelaySet(duration: Duration) extends InternalEvent with BackendEvent

  case class Killed(actorRef: EventActorRef) extends InternalEvent with BackendEvent with FilteredActorEvent

  case class ActorFailure(
    actorRef: EventActorRef,
    cause: Throwable,
    decision: String
  ) extends InternalEvent with BackendEvent

  case object ReportingEnabled extends InternalEvent with BackendEvent

  case object ReportingDisabled extends InternalEvent with BackendEvent

  case class SnapshotAvailable(snapshot: LightSnapshot) extends BackendEvent

  case class LightSnapshot(
      liveActors: Set[String] = Set(),
      children: Map[String, Set[String]] = Map(),
      receivedFrom: Set[(String, String)] = Set()
  ) {

    implicit def eventActorRef2String(eventActorRef: EventActorRef): String = eventActorRef.path

    def dead: Set[String] = {
      liveActors diff (children.values.flatten ++ receivedFrom.flatMap(p => Seq(p._1, p._2))).toSet
    }

    def update(ev: BackendEvent): LightSnapshot = ev match {
      case ReceivedWithId(_, from, to, _) =>
        val live: Set[String] = liveActors ++ Set(from, to).filter(_.isUserActor).map(_.path)
        val recv = receivedFrom + (from.path -> to.path)
        copy(liveActors = live, receivedFrom = recv)
      case Spawned(ref, parent) =>
        if (ref.isUserActor) {
          val live = liveActors + ref
          val childr = children.updated(parent, children.getOrElse(parent, Set()) + ref)
          copy(liveActors = live, children = childr)
        } else {
          this
        }
      case Killed(ref) if ref.isUserActor =>
        copy(liveActors = liveActors - ref)
      case CurrentActorState(ref, _) if ref.isUserActor =>
        copy(liveActors = liveActors + ref)
      case Instantiated(ref, _) if ref.isUserActor =>
        copy(liveActors = liveActors + ref)
      case other =>
        this
    }
  }

}