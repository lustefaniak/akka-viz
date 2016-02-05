package akka.viz.events

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.viz.events.types.{AvailableMessageTypes, BackendEvent, Received, ReceivedWithId}

import scala.collection.immutable

class EventPublisherActor extends Actor with ActorLogging {

  var subscribers = immutable.Set[ActorRef]()
  var availableTypes = immutable.Set[Class[_ <: Any]]()
  var eventCounter = 0L

  override def receive: Receive = {
    case r: Received =>
      trackMsgType(r.message)
      broadcast(ReceivedWithId(nextEventNumber(), r.sender, r.receiver, r.message))

    case be: BackendEvent =>
      broadcast(be)

    case EventPublisherActor.Subscribe =>
      val s = sender()
      subscribers += s
      context.watch(s)
      s ! AvailableMessageTypes(availableTypes.toList)

    case EventPublisherActor.Unsubscribe =>
      unsubscribe(sender())

    case Terminated(s) =>
      unsubscribe(s)
  }

  def broadcast(backendEvent: BackendEvent): Unit = {
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
      subscribers.foreach(_ ! AvailableMessageTypes(availableTypes.toList))
    }
  }
}

object EventPublisherActor {

  case object Subscribe

  case object Unsubscribe

}
