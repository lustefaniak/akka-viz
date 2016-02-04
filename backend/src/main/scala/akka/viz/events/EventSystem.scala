package akka.viz.events

import akka.actor._
import akka.util.Timeout
import akka.viz.config.Config
import akka.viz.events.GlobalSettingsActor.GetDelay
import akka.viz.events.types._

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern._

object EventSystem {

  implicit val timeout = Timeout(100.millis)
  private implicit val system = ActorSystem(Config.internalSystemName)

  private val publisher = system.actorOf(Props(classOf[EventPublisherActor]))
  private val globalSettings = system.actorOf(Props(classOf[GlobalSettingsActor]))

  globalSettings ! publisher

  private[akka] def receiveDelay = {
    Await.result((globalSettings ? GlobalSettingsActor.GetDelay).mapTo[Duration], timeout.duration)
  }

  private[akka] def receiveDelay_=(d: Duration): Unit = {
    globalSettings ! d
  }

  private[akka] def publish(event: InternalEvent): Unit = {
    publisher ! event
  }

  def subscribe(subscriber: ActorRef): Unit = {
    publisher.tell(EventPublisherActor.Subscribe, subscriber)
  }

}

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

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.viz.events.types.ReceiveDelaySet

import scala.concurrent.duration._

/* Ugly singleton thing for things that cannot be done per-client,
   but are not concerned with processing of the events.
  */
class GlobalSettingsActor extends Actor with ActorLogging {
  private[this] var rcvDelay: FiniteDuration = 0.millis
  private[this] var eventPublisher: Option[ActorRef] = None

  override def receive: Receive = {
    case d: FiniteDuration =>
      rcvDelay = d
      eventPublisher.foreach(_ ! ReceiveDelaySet(d))

    case GetDelay =>
      sender() ! rcvDelay

    case publisher: ActorRef =>
      eventPublisher = Some(publisher)
  }
}

object GlobalSettingsActor {
  case object GetDelay
}