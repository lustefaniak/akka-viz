package akkaviz.events

import akka.actor._
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akkaviz.config.Config
import akkaviz.events.EventPublisherActor.Subscribe
import akkaviz.events.types._
import org.scalatest.{Matchers, WordSpecLike}

class EventPublisherActorTest extends TestKit(ActorSystem("EventPublisherActorTestSystem")) with ImplicitSender
  with WordSpecLike with Matchers {
  import scala.concurrent.duration._

  val someActorRef = TestActorRef(new Actor { def receive = { case _ => ()}})

  "EventPublisherActor" should {
    "not publish any events if monitoring is disabled" in {
      val publisher = system.actorOf(Props(classOf[EventPublisherActor], () => false, Config.maxEventsInSnapshot))
      publisher ! Subscribe
      expectMsgAllOf(
        ReportingDisabled,
        SnapshotAvailable(LightSnapshot()),
        AvailableMessageTypes(Set())
      )
      system.stop(publisher)
    }

    "publish events and update message types when monitoring is enabled" in {
      val publisher = system.actorOf(Props(classOf[EventPublisherActor], () => true, Config.maxEventsInSnapshot))
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

    "rewrite Received as ReceivedWithId" in {
      val publisher = system.actorOf(Props(classOf[EventPublisherActor], () => true, Config.maxEventsInSnapshot))

      val originalReceived = Received(ActorRef.noSender, someActorRef, "hello", true)

      publisher ! Subscribe
      publisher ! originalReceived

      val rewritten = fishForMessage(1.second, "didn't broadcast ReceivedWithId!") {
        case ri: ReceivedWithId => true
        case _ => false
      }.asInstanceOf[ReceivedWithId]

      rewritten.actorRef should equal(originalReceived.actorRef)
      rewritten.sender should equal(originalReceived.sender)
      rewritten.handled should equal(originalReceived.handled)
      rewritten.message should equal(originalReceived.message)

      system.stop(publisher)
    }
  }
}
