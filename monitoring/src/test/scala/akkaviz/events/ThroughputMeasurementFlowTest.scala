package akkaviz.events

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.{TestActorRef, TestKit}
import akkaviz.events.types.{BackendEvent, ReceivedWithId, ThroughputMeasurement}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Future

class ThroughputMeasurementFlowTest extends TestKit(ActorSystem("FlowTestSystem"))
    with WordSpecLike with Matchers with ScalaFutures {

  import scala.concurrent.duration._

  implicit val materializer = ActorMaterializer()(system)

  val firstRef = TestActorRef[SomeActor](new SomeActor, "first")
  val secondRef = TestActorRef[SomeActor](new SomeActor, "second")

  override implicit val patienceConfig = PatienceConfig(timeout = 5.seconds)

  "ThroughputMeasurementFlow" should {

    "not emit any measurements if there are no Received events" in {
      val src = Source.empty[BackendEvent]
      val sink: Sink[BackendEvent, Future[List[BackendEvent]]] = Sink.fold(List.empty[BackendEvent])((list, ev) => ev :: list)

      val materialized = ThroughputMeasurementFlow(1.second).runWith(src, sink)._2

      whenReady(materialized) { r =>
        r should be('empty)
      }
    }

    "emit proper measured value for one message" in {
      val src = Source.single(ReceivedWithId(1, ActorRef.noSender, firstRef, "sup", true))
      val mat = src.via(ThroughputMeasurementFlow(1.second))
        .toMat(Sink.head[ThroughputMeasurement])(Keep.right).run()

      whenReady(mat) { measurement =>
        measurement.actorRef should equal(firstRef)
        measurement.msgsPerSecond should equal(1.0)
      }
    }

    "emit measured value for one message and 0 for actors which didn't receive anything" in {
      import system.dispatcher
      val src = Source(List(
        ReceivedWithId(1, ActorRef.noSender, firstRef, "sup", true),
        ReceivedWithId(2, ActorRef.noSender, secondRef, "sup", true)
      )).concat(Source.fromFuture(pattern.after(2.seconds, system.scheduler) {
        Future.successful(ReceivedWithId(3, ActorRef.noSender, firstRef, "sup", true))
      }))

      val mat = src.via(ThroughputMeasurementFlow(1.second))
        .toMat(Sink.fold(List.empty[ThroughputMeasurement]) { (list, ev) => ev :: list })(Keep.right).run()

      whenReady(mat) { measurements =>
        val measurementsFor = measurements.groupBy(_.actorRef)
        measurementsFor(firstRef).map(_.msgsPerSecond) should not contain 0.0
        measurementsFor(secondRef).sortBy(_.timestamp).map(_.msgsPerSecond) should contain inOrder (1.0, 0.0)
      }
    }
  }
}
