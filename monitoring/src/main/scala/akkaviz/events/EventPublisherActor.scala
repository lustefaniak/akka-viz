package akkaviz.events

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akkaviz.events.types._

import scala.collection.immutable
import scala.collection.immutable.Queue

class EventPublisherActor extends Actor with ActorLogging {

  var subscribers = immutable.Set[ActorRef]()
  var availableTypes = immutable.Set[Class[_ <: Any]]()
  var eventCounter = 0L
  var snapshot: LightSnapshot = LightSnapshot()
  var snapshotQueue = immutable.Queue.empty[BackendEvent]

  override def receive = monitoringReceive

  def disabledReceive: Receive = handleSubscribe orElse {
    case re @ ReportingEnabled =>
      broadcast(re)
      context.become(monitoringReceive)
  }

  def monitoringReceive: Receive = handleSubscribe orElse {
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

  def handleSubscribe: PartialFunction[Any, Unit] = {
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
    s ! (if (EventSystem.isEnabled()) ReportingEnabled else ReportingDisabled)
    s ! AvailableMessageTypes(availableTypes)
    s ! SnapshotAvailable(snapshot)
    snapshotQueue.foreach(s ! _)
  }

  def broadcast(backendEvent: BackendEvent): Unit = {
    subscribers.foreach(_ ! backendEvent)
  }

  def collectForSnapshot: PartialFunction[Any, Any] = {
    case r: Received =>
      collectForSnapshot(ReceivedWithId(nextEventNumber(), r.sender, r.actorRef, r.message, r.handled))

    case ev: BackendEvent if snapshotQueue.size == EventPublisherActor.EventsForSnaphot =>
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

  val EventsForSnaphot = 200

}
