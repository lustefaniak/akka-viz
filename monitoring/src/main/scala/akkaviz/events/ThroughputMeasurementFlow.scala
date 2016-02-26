package akkaviz.events

import akka.actor.ActorRef
import akka.stream.scaladsl.Flow
import akkaviz.events.types.{ThroughputMeasurement, ReceivedWithId, BackendEvent}

import scala.concurrent.duration._

object ThroughputMeasurementFlow {
  def apply(period: FiniteDuration): Flow[BackendEvent, ThroughputMeasurement, Any] = {
    Flow[BackendEvent]
      .collect { case r: ReceivedWithId => r.actorRef }
      .groupedWithin(Int.MaxValue, period)
      .map { refs =>
        refs.groupBy(identity).mapValues(_.length)
      }
      .scan(Map[ActorRef, Int]()) { case (previous, current) =>
        // produce zero for actors that have been measured previously but didn't receive any messages during `period`
        current ++ (for { k <- current.keySet.diff(previous.keySet) } yield k -> 0)
      }
      .mapConcat { m =>
        for {
          (ref, count) <- m
        } yield ThroughputMeasurement(ref, count / (period.toMillis.toDouble / 1.second.toMillis.toDouble))
      }
  }
}

