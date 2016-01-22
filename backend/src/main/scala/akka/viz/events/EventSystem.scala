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

class EventPublisherActor extends Actor {
  val maxElementsInQueue = Config.eventsToReply
  var queue = immutable.Queue[Event]()
  var subscribers = immutable.Set[ActorRef]()

  override def receive: Receive = {
    case event: Event =>
      if (EventFiltering.isAllowed(event)) {
        queue = queue.enqueueFinite(event, maxElementsInQueue)
        subscribers.foreach(_ ! event)
      }
    case Subscribe =>
      val s = sender()
      subscribers += s
      queue.foreach(s ! _)
    case Unsubscribe =>
      subscribers -= sender()
  }
}