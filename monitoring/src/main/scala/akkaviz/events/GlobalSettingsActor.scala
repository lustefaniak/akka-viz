package akkaviz.events

import akka.actor.{Actor, ActorLogging, ActorRef}
import akkaviz.events.GlobalSettingsActor.GetDelay
import akkaviz.events.types.ReceiveDelaySet

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
