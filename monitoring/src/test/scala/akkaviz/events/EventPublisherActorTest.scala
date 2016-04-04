package akkaviz.events

import akka.actor._
import akka.dispatch.sysmsg.DeathWatchNotification
import akka.testkit.{TestProbe, TestActorRef, ImplicitSender, TestKit}
import akkaviz.config.Config
import akkaviz.events.EventPublisherActor.{Unsubscribe, Subscribe}
import akkaviz.events.types._
import org.scalatest.{Matchers, WordSpecLike}

class EventPublisherActorTest extends TestKit(ActorSystem("EventPublisherActorTestSystem")) with ImplicitSender
    with WordSpecLike with Matchers with akkaviz.events.Helpers {

  import scala.concurrent.duration._

  val someActorRef = TestActorRef(new Actor { def receive = { case _ => () } }, "someActor")

  def withPublisher(enabledTest: () => Boolean = () => true)(test: (ActorRef) => Any) = {
    val publisher = system.actorOf(Props(classOf[EventPublisherActor], enabledTest, Config.maxEventsInSnapshot))
    try { test(publisher) }
    finally system.stop(publisher)
  }

  "EventPublisherActor" should {
    "not publish any events if monitoring is disabled" in withPublisher() { publisher =>
      publisher ! Subscribe
      expectMsgAllOf(
        ReportingDisabled,
        SnapshotAvailable(LightSnapshot()),
        AvailableMessageTypes(Set())
      )
      system.stop(publisher)
    }

    "publish events and update message types when monitoring is enabled" in withPublisher() { publisher =>
      publisher ! Subscribe
      val receivedWithId = ReceivedWithId(1, ActorRef.noSender, someActorRef, "\'ello", true)
      publisher ! receivedWithId

      expectMsgAllOf(
        ReportingEnabled,
        SnapshotAvailable(LightSnapshot()),
        AvailableMessageTypes(Set()),
        receivedWithId,
        AvailableMessageTypes(Set(classOf[String]))
      )

      system.stop(publisher)
    }

    "rewrite Received as ReceivedWithId" in withPublisher() { publisher =>

      val originalReceived = Received(ActorRef.noSender, someActorRef, "hello", true)

      publisher ! Subscribe
      publisher ! originalReceived

      val rewritten = fishForMessage(1.second, "didn't broadcast ReceivedWithId!") {
        case ri: ReceivedWithId => true
        case _                  => false
      }.asInstanceOf[ReceivedWithId]

      rewritten.actorRef should equal(originalReceived.actorRef)
      rewritten.sender should equal(originalReceived.sender)
      rewritten.handled should equal(originalReceived.handled)
      rewritten.message should equal(originalReceived.message)

      system.stop(publisher)
    }

    var isEnabled = true
    "handle monitoring status changes" in withPublisher(() => isEnabled) { publisher =>
      publisher ! Subscribe
      fishForMessage(100.millis, "didn't receive ReportingEnabled") {
        case ReportingEnabled => true
        case _                => false
      }

      isEnabled = false
      publisher ! ReportingDisabled
      fishForMessage(100.millis, "didn't receive ReportingDisabled after status change") {
        case ReportingDisabled => true
        case _                 => false
      }
    }

    "build snapshots after collecting configured amount of BackendEvents" in withPublisher() { publisher =>
      for (n <- 1 to Config.maxEventsInSnapshot) publisher ! Received(ActorRef.noSender, someActorRef, n, true)

      publisher ! Subscribe
      val snapshotAvailable = fishForMessage(100.millis, "didn't receive snapshot") {
        case SnapshotAvailable(_) => true
        case _                    => false
      }.asInstanceOf[SnapshotAvailable]

      snapshotAvailable.snapshot.liveActors should contain(actorRefToString(someActorRef))
      snapshotAvailable.snapshot.dead should be('empty)

    }

    "send no messages after Unsubscribe" in withPublisher() { publisher =>
      publisher ! Subscribe

      expectMsgAllConformingOf(
        1.second,
        classOf[AvailableMessageTypes], classOf[SnapshotAvailable], ReportingEnabled.getClass
      )

      publisher ! Unsubscribe
      publisher ! Instantiated(someActorRef, someActorRef.underlyingActor)

      expectNoMsg(1.second)
      system.stop(publisher)
    }

    "handle termination of subscriber" in withPublisher() { publisher =>
      val probe = TestProbe("terminationTestProbe")

      publisher.tell(Subscribe, probe.ref)
      system.eventStream.subscribe(testActor, classOf[DeadLetter])

      probe.expectMsgAllConformingOf(
        1.second,
        classOf[AvailableMessageTypes], classOf[SnapshotAvailable], ReportingEnabled.getClass
      )

      probe.ref ! PoisonPill

      publisher ! Instantiated(someActorRef, someActorRef.underlyingActor)

      expectNoMsg(1.second)
      system.eventStream.unsubscribe(testActor)
    }

    "track message types" in withPublisher() { publisher =>

      val received = Seq(
        Received(someActorRef, someActorRef, 123, true),
        Received(someActorRef, someActorRef, "abc", true),
        Received(someActorRef, someActorRef, Some("xyz"), true)
      )

      received.foreach(publisher ! _)

      publisher ! Subscribe

      fishForMessage(100.millis, "did not get expected available types") {
        case AvailableMessageTypes(_) => true
        case _                        => false
      }.asInstanceOf[AvailableMessageTypes].classes should contain allElementsOf received.map(_.message.getClass)

    }

  }
}
