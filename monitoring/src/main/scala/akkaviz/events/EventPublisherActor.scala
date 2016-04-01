package akkaviz.events

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akkaviz.events.types._

import scala.collection.immutable
import scala.collection.immutable.Queue

class EventPublisherActor(
    monitoringEnabled: () => Boolean, maxEventsInSnapshot: Int
) extends Actor with ActorLogging {

  private[this] var subscribers = immutable.Set[ActorRef]()
  private[this] var availableTypes = immutable.Set[Class[_ <: Any]]()
  private[this] var eventCounter = 0L
  private[this] var snapshot: LightSnapshot = LightSnapshot()
  private[this] var snapshotQueue = immutable.Queue.empty[BackendEvent]

  override def receive = monitoringReceive

  private[this] def disabledReceive: Receive = handleSubscribe orElse {
    case re @ ReportingEnabled =>
      broadcast(re)
      context.become(monitoringReceive)
  }

  private[this] def monitoringReceive: Receive = handleSubscribe orElse {
    collectForSnapshot andThen {
      case rd @ ReportingDisabled =>
        broadcast(rd)
        context.become(disabledReceive)

      case r: ReceivedWithId =>
        trackMsgType(r.message)
        broadcast(r)

      case be: BackendEvent =>
        broadcast(be)
    }
  }

  private[this] def handleSubscribe: PartialFunction[Any, Unit] = {
    case EventPublisherActor.Subscribe =>
      val s = sender()
      addSubscriberAndInit(s)

    case EventPublisherActor.Unsubscribe =>
      unsubscribe(sender())

    case Terminated(s) =>
      unsubscribe(s)
  }

  def addSubscriberAndInit(s: ActorRef): Unit = {
    subscribers += s
    context.watch(s)
    s ! (if (monitoringEnabled()) ReportingEnabled else ReportingDisabled)
    s ! AvailableMessageTypes(availableTypes)
    s ! SnapshotAvailable(snapshot)
    snapshotQueue.foreach(s ! _)
  }

  private[this] def broadcast(backendEvent: BackendEvent): Unit = {
    subscribers.foreach(_ ! backendEvent)
  }

  private[this] def collectForSnapshot: PartialFunction[Any, Any] = {
    case r: Received =>
      collectForSnapshot(ReceivedWithId(nextEventNumber(), r.sender, r.actorRef, r.message, r.handled))

    case ev: BackendEvent if snapshotQueue.size == maxEventsInSnapshot =>
      snapshot = snapshotQueue.enqueue(ev).foldLeft(snapshot) {
        _.update(_)
      }
      snapshotQueue = Queue.empty
      ev
    case ev: BackendEvent =>
      snapshotQueue = snapshotQueue.enqueue(ev)
      ev
    case other =>
      other
  }

  @inline
  private[this] def nextEventNumber(): Long = {
    eventCounter += 1
    eventCounter
  }

  private[this] def unsubscribe(s: ActorRef): Unit = {
    subscribers -= s
  }

  private[this] def trackMsgType(msg: Any): Unit = {
    if (!availableTypes.contains(msg.getClass)) {
      availableTypes += msg.getClass
      subscribers.foreach(_ ! AvailableMessageTypes(availableTypes))
    }
  }
}

object EventPublisherActor {

  case object Subscribe

  case object Unsubscribe

}
