package akka.viz.events

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.viz.events.types._

import scala.collection.immutable
import scala.collection.immutable.Queue

class EventPublisherActor extends Actor with ActorLogging {

  var subscribers = immutable.Set[ActorRef]()
  var availableTypes = immutable.Set[Class[_ <: Any]]()
  var eventCounter = 0L
  var snapshot: LightSnapshot = LightSnapshot()
  var snapshotQueue = immutable.Queue.empty[InternalEvent]

  override def receive = collectForSnapshot andThen {
    case r: Received =>
      trackMsgType(r.message)
      broadcast(ReceivedWithId(nextEventNumber(), r.sender, r.receiver, r.message))

    case be: BackendEvent =>
      broadcast(be)

    case EventPublisherActor.Subscribe =>
      val s = sender()
      subscribers += s
      context.watch(s)
      s ! (if (EventSystem.isEnabled()) ReportingEnabled else ReportingDisabled)
      s ! AvailableMessageTypes(availableTypes.toList)
      s ! SnapshotAvailable(snapshot)
      snapshotQueue.foreach(s ! _)

    case EventPublisherActor.Unsubscribe =>
      unsubscribe(sender())

    case Terminated(s) =>
      unsubscribe(s)
  }

  def broadcast(backendEvent: BackendEvent): Unit = {
    subscribers.foreach(_ ! backendEvent)
  }

  def collectForSnapshot: PartialFunction[Any, Any] = {
    case ev: InternalEvent if snapshotQueue.size == EventPublisherActor.EventsForSnaphot =>
      snapshot = snapshotQueue.enqueue(ev).foldLeft(snapshot) {
        _.update(_)
      }
      snapshotQueue = Queue.empty
      ev
    case ev: InternalEvent =>
      snapshotQueue = snapshotQueue.enqueue(ev)
      ev
  }

  @inline
  private def nextEventNumber(): Long = {
    eventCounter += 1
    eventCounter
  }

  private def unsubscribe(s: ActorRef): Unit = {
    subscribers -= s
  }

  private def trackMsgType(msg: Any): Unit = {
    if (!availableTypes.contains(msg.getClass)) {
      availableTypes += msg.getClass
      subscribers.foreach(_ ! AvailableMessageTypes(availableTypes.toList))
    }
  }
}

object EventPublisherActor {

  case object Subscribe

  case object Unsubscribe

  val EventsForSnaphot = 200

}
