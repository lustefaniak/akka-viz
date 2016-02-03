package akka.viz.events

import akka.actor._
import akka.viz.config.Config
import akka.viz.util.FiniteQueue._

import scala.collection.immutable

object EventSystem {

  private implicit val system = ActorSystem(Config.internalSystemName)
  private val publisher = system.actorOf(Props(classOf[EventPublisherActor]))

  private[akka] def publish(event: internal.Event): Unit = {
    publisher ! event
  }

  def subscribe(subscriber: ActorRef): Unit = {
    publisher.tell(EventPublisherActor.Subscribe, subscriber)
  }

}

class EventPublisherActor extends Actor with ActorLogging {

  val maxElementsInQueue = Config.eventsToReply
  var queue = immutable.Queue[backend.Event]()
  var subscribers = immutable.Set[ActorRef]()

  var availableTypes = immutable.Set[Class[_ <: Any]]()

  var eventCounter = 0L

  override def receive: Receive = {
    case r: internal.Received =>
      trackMsgType(r.message)
      enqueueAndPublish(backend.Received(nextEventNumber(), r.sender, r.receiver, r.message))

    case s: internal.Spawned =>
      enqueueAndPublish(backend.Spawned(nextEventNumber(), s.ref, s.parent))

    case mb: internal.MailBoxStatus =>
      enqueueAndPublish(backend.MailboxStatus(nextEventNumber(), mb.owner, mb.size))

    case i: internal.Instantiated =>
      enqueueAndPublish(backend.Instantiated(nextEventNumber(), i.actorRef, i.actor.getClass))

    case t: internal.FSMTransition =>
      enqueueAndPublish(backend.FSMTransition(nextEventNumber(), t.actorRef, t.currentState, t.currentData, t.nextState, t.nextData))

    case EventPublisherActor.Subscribe =>
      val s = sender()
      subscribers += s
      context.watch(s)
      s ! backend.AvailableMessageTypes(availableTypes.toList)
      queue.foreach(s ! _)

    case EventPublisherActor.Unsubscribe =>
      unsubscribe(sender())

    case Terminated(s) =>
      unsubscribe(s)
  }

  def enqueueAndPublish(backendEvent: backend.Event): Unit = {
    queue = queue.enqueueFinite(backendEvent, maxElementsInQueue)
    subscribers.foreach(_ ! backendEvent)
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
      subscribers.foreach(_ ! backend.AvailableMessageTypes(availableTypes.toList))
    }
  }
}

object EventPublisherActor {

  case object Subscribe

  case object Unsubscribe

}