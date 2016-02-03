package akka.viz.manipulation

import akka.actor.ActorRef
import akka.viz.events.internal.ReceiveDelaySet

import scala.concurrent.duration._

case class ReceiveDelay(duration: FiniteDuration = 0.millis) extends AnyVal

trait SystemManipulation {
  def receiveDelay : ReceiveDelay

  def receiveDelay_= : (ReceiveDelay => Unit)

  def publisherActor_= : (ActorRef => Unit)
}

/* Ugly singleton thing (supposed to be wrapped in a TypedActor), but there's no way this could be done per-client.
   Use with care. Embrace TimeoutExceptions.
 */
class SystemManipulationImpl extends SystemManipulation {
  private[this] var rcvDelay = new ReceiveDelay()
  private[this] var eventPublisher: Option[ActorRef] = None

  override def receiveDelay: ReceiveDelay = rcvDelay

  override def receiveDelay_= : (ReceiveDelay) => Unit = { v =>
    rcvDelay = v
    eventPublisher.foreach(_ ! ReceiveDelaySet(rcvDelay))
  }

  override def publisherActor_= : (ActorRef) => Unit = {  ref =>
    eventPublisher = Some(ref)
    ref ! ReceiveDelaySet(rcvDelay)
  }
}

