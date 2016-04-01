package akkaviz.server

import akka.actor.Actor.Receive
import akka.actor.{SupervisorStrategy, Props, Actor, ActorSystem}
import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akkaviz.events.{LightSnapshot, Helpers}
import akkaviz.events.types._
import akkaviz.protocol
import akkaviz.protocol.ApiServerMessage
import akkaviz.serialization.MessageSerialization
import org.scalatest.{AsyncFunSuite, BeforeAndAfterAll, Matchers, FunSuite}

import scala.concurrent.Await
import scala.concurrent.duration._

class BackendEventsMarshallingTest extends AsyncFunSuite with Matchers with BeforeAndAfterAll {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  private[this] var actorInstance: Option[Actor] = None
  private[this] val actorRef = system.actorOf(Props(new Actor {
    actorInstance = Some(this)

    override def receive: Receive = {
      case x => ???
    }
  }))
  private[this] val actorRefString = Helpers.actorRefToString(actorRef)

  private[this] def marshal(backendEvent: BackendEvent) = {
    Source.single(backendEvent).via(BackendEventsMarshalling.backendEventToProtocolFlow).runWith(Sink.head)
  }

  test("Could obtain direct access to Actor instance") {
    actorInstance.isDefined shouldBe true
  }

  test("ReceivedWithId") {
    marshal(ReceivedWithId(1, actorRef, actorRef, 1, true)) map {
      _ shouldBe protocol.Received(1, actorRefString, actorRefString, "java.lang.Integer", Some("1"), true)
    }
  }

  test("AvailableMessageTypes") {
    marshal(AvailableMessageTypes(Set(BigDecimal(0).getClass))) map {
      _ shouldBe protocol.AvailableClasses(Set("scala.math.BigDecimal"))
    }
  }
  test("Spawned") {
    marshal(Spawned(actorRef)) map {
      _ shouldBe protocol.Spawned(actorRefString)
    }
  }

  test("ActorSystemCreated") {
    marshal(ActorSystemCreated(system)) map {
      _ shouldBe protocol.ActorSystemCreated(system.name)
    }
  }

  test("Instantiated") {
    val actor = actorInstance.getOrElse(fail)
    marshal(Instantiated(actorRef, actor)) map {
      _ shouldBe protocol.Instantiated(actorRefString, actor.getClass.getName)
    }
  }

  test("FSMTransition") {
    marshal(FSMTransition(actorRef, 1, 2, 10, 20)) map {
      val jlI = "java.lang.Integer"
      _ shouldBe protocol.FSMTransition(actorRefString, "1", jlI, "2", jlI, "10", jlI, "20", jlI)
    }
  }
  test("CurrentActorState") {
    marshal(CurrentActorState(actorRef, actorInstance.getOrElse(fail))) map {
      m =>
        m shouldBe a[protocol.CurrentActorState]
        m.asInstanceOf[protocol.CurrentActorState].ref shouldBe actorRefString
    }
  }
  test("MailboxStatus") {
    marshal(MailboxStatus(actorRef, 42)) map {
      _ shouldBe protocol.MailboxStatus(actorRefString, 42)
    }
  }
  test("ReceiveDelaySet") {
    marshal(ReceiveDelaySet(10.millis)) map {
      _ shouldBe protocol.ReceiveDelaySet(10.millis)
    }
  }
  test("Killed") {
    marshal(Killed(actorRef)) map {
      _ shouldBe protocol.Killed(actorRefString)
    }
  }
  test("ActorFailure") {
    val ts = System.currentTimeMillis()
    val ex = new Exception("test")
    val exString = MessageSerialization.render(ex)
    marshal(ActorFailure(actorRef, ex, SupervisorStrategy.Stop, ts)) map {
      _ shouldBe protocol.ActorFailure(actorRefString, exString, "Stop", ts)
    }
  }
  test("Question") {
    marshal(Question(1, Some(actorRef), actorRef, 123)) map {
      _ shouldBe protocol.Question(1, Some(actorRefString), actorRefString, "123")
    }
  }
  test("Answer") {
    marshal(Answer(1, 123)) map {
      _ shouldBe protocol.Answer(1, "123")
    }
  }
  test("AnswerFailed") {
    val ex = new Exception("test")
    val exString = MessageSerialization.render(ex)
    marshal(AnswerFailed(1, ex)) map {
      _ shouldBe protocol.AnswerFailed(1, exString)
    }
  }
  test("ReportingDisabled") {
    marshal(ReportingDisabled) map {
      _ shouldBe protocol.ReportingDisabled
    }
  }
  test("ReportingEnabled") {
    marshal(ReportingEnabled) map {
      _ shouldBe protocol.ReportingEnabled
    }
  }
  test("SnapshotAvailable") {
    val snap = LightSnapshot(Set(actorRefString), Set((actorRefString, actorRefString)), Map((actorRefString, "SomeClass")))

    marshal(SnapshotAvailable(snap)) map {
      _ shouldBe protocol.SnapshotAvailable(Map((actorRefString, Some("SomeClass"))), Map(), Set((actorRefString, actorRefString)))
    }
  }
  test("ThroughputMeasurement") {
    val ts = System.currentTimeMillis()
    marshal(ThroughputMeasurement(actorRef, 42, ts)) map {
      _ shouldBe protocol.ThroughputMeasurement(actorRefString, 42, ts)
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    Await.result(system.terminate(), Duration.Inf)
  }
}
