package akka.viz.events

import akka.actor.Actor.Receive
import akka.actor._
import akka.event.{ActorEventBus, EventBus}
import akka.stream.Materializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import akka.viz.config.Config
import akka.viz.events.EventSystem.{Subscribe, Unsubscribe}
import org.reactivestreams.Publisher
import scala.collection.immutable
import akka.viz.util.FiniteQueue._


sealed trait Event

case class Received(sender: ActorRef, receiver: ActorRef, message: Any) extends Event
case class AvailableMessageTypes(classes: List[Class[_ <: Any]]) extends Event

object EventSystem {

  case object Subscribe
  case object Unsubscribe

  private implicit val system = ActorSystem(Config.internalSystemName)
  private val publisher = system.actorOf(Props(classOf[EventPublisherActor]))

  private[akka] def publish(event: Event): Unit = {
    publisher ! event
  }

  def subscribe(subscriber: ActorRef): Unit = {
    publisher.tell(Subscribe, subscriber)
  }

}

class EventPublisherActor extends Actor with ActorLogging {
  val maxElementsInQueue = Config.eventsToReply
  var queue = immutable.Queue[Event]()
  var subscribers = immutable.Set[ActorRef]()

  var availableTypes = immutable.Set[Class[_ <: Any]]()
  var allowed: FilteringRule = FilteringRule.Default

  override def receive: Receive = {
    case rule: FilteringRule =>
      log.info(s"updated FilteringRule: $rule")
      allowed = rule

    case r @ Received(_, _, msg) =>
      trackMsgType(msg)

      if (allowed(r)) {
        queue = queue.enqueueFinite(r, maxElementsInQueue)
        subscribers.foreach(_ ! r)
      }

    case Subscribe =>
      val s = sender()
      subscribers += s
      context.watch(s)
      s ! AvailableMessageTypes(availableTypes.toList)
      queue.foreach(s ! _)
    case Unsubscribe =>
      unsubscribe(sender())
    case Terminated(s) =>
      unsubscribe(s)
  }

  def unsubscribe(s: ActorRef): Unit = {
    subscribers -= s
  }

  def trackMsgType(msg: Any): Unit = {
    if (!availableTypes.contains(msg.getClass)) {
      availableTypes += msg.getClass
      subscribers.foreach(_ ! AvailableMessageTypes(availableTypes.toList))
    }
  }
}