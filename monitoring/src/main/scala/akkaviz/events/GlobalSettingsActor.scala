package akkaviz.events

import akka.actor._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import akkaviz.config.Config
import akkaviz.events.GlobalSettingsActor.{DisableThroughput, EnableThroughput, GetDelay}
import akkaviz.events.types.{ThroughputMeasurement, BackendEvent, ReceiveDelaySet}

import scala.concurrent.duration._

/* Ugly singleton thing for things that cannot (or shouldn't) be done per-client,
   but are not concerned with publishing of the events.
  */
class GlobalSettingsActor extends Actor with ActorLogging {
  private[this] var rcvDelay: FiniteDuration = 0.millis
  private[this] var eventPublisher: Option[ActorRef] = None
  private[this] var throughputSrcRef: Option[ActorRef] = None

  implicit val mat = ActorMaterializer()

  override def receive: Receive = {
    case d: FiniteDuration =>
      rcvDelay = d
      eventPublisher.foreach(_ ! ReceiveDelaySet(d))

    case GetDelay =>
      sender() ! rcvDelay

    case publisher: ActorRef =>
      eventPublisher = Some(publisher)
      self ! EnableThroughput // todo get from config (could be on by default)

    case EnableThroughput =>
      val src = Source.actorRef[BackendEvent](Config.bufferSize, OverflowStrategy.dropHead)
        .mapMaterializedValue(EventSystem.subscribe)
      val sink = Sink.foreach[ThroughputMeasurement](ev => EventSystem.report(ev))
      val flow = src.via(ThroughputMeasurementFlow.apply(1.second)).to(sink).run()

    case DisableThroughput =>
      throughputSrcRef.foreach { ref =>
        ref ! PoisonPill
        throughputSrcRef = None
      }

  }
}

object GlobalSettingsActor {

  case object GetDelay
  case object EnableThroughput
  case object DisableThroughput
}
