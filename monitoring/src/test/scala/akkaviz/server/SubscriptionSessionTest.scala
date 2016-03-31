package akkaviz.server

import akka.actor.ActorSystem
import akkaviz.events.{LightSnapshot, Helpers}
import akkaviz.events.types._
import akkaviz.protocol
import org.scalatest.{Matchers, BeforeAndAfterAll, FunSuite}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SubscriptionSessionTest extends FunSuite with SubscriptionSession with BeforeAndAfterAll with Matchers {

  val system = ActorSystem()
  val actorRef = system.deadLetters
  val actorRefString = Helpers.actorRefToString(actorRef)
  val receivedMessage = ReceivedWithId(1, actorRef, actorRef, BigDecimal(1.1), true)

  test("Default filtering of SubscriptionSession") {
    defaultSettings.eventAllowed(Killed(actorRef)) shouldBe false
    defaultSettings.eventAllowed(receivedMessage) shouldBe false
  }

  test("Non filtered messages are passed through") {
    val nonFilteredMessages = Seq(
      Spawned(actorRef),
      ReceiveDelaySet(Duration.Inf),
      ReportingEnabled,
      ReportingDisabled,
      SnapshotAvailable(LightSnapshot())
    )

    nonFilteredMessages.foreach {
      defaultSettings.eventAllowed(_) shouldBe true
    }
  }

  test("Allowing some actor makes it accepted") {
    val session = updateSettings(defaultSettings, SetActorEventFilter(Set(actorRefString)))
    session.eventAllowed(Killed(actorRef)) shouldBe true // only ReceivedWithId is verified
    session.eventAllowed(receivedMessage) shouldBe false
  }

  test("Allowing some actor and message type makes it accepted") {
    val session = Seq(SetActorEventFilter(Set(actorRefString)), SetAllowedClasses(Set(receivedMessage.message.getClass.getName)))
      .foldLeft(defaultSettings)(updateSettings)
    session.eventAllowed(receivedMessage) shouldBe true
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    Await.result(system.terminate(), Duration.Inf)
  }
}
