package akka.viz.serialization

import akka.actor.{ActorSystem, Actor, ActorRef}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import akka.viz.config.Config
import akka.viz.events.EventSystem
import akka.viz.events.types._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.duration._
import scala.concurrent.Future

class ReplayablesTest extends FlatSpec with Matchers with ScalaFutures {
  "EventSystem" should "store and replay important events" in {
    val system: ActorSystem = ActorSystem(Config.internalSystemName)
    implicit val materializer = ActorMaterializer()(system)

    val replayables: Vector[InternalEvent] = Vector(
      Spawned(ActorRef.noSender, null),
      Instantiated(ActorRef.noSender, null))

    replayables foreach EventSystem.publish

    val fold: Sink[BackendEvent, Future[Vector[BackendEvent]]] = Sink.fold(Vector.empty[BackendEvent])(_ :+ _)

    val future = Source.actorRef(200, OverflowStrategy.fail)
      .mapMaterializedValue(EventSystem.subscribe)
      .takeWithin(2.seconds)
      .toMat(fold) {case (_, m) => m}.run()

    whenReady(future, Timeout(3.seconds)) { result =>
      result should contain allElementsOf replayables
    }

  }
}
