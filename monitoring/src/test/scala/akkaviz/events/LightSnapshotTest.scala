package akkaviz.events

import akka.actor.{ActorSystem, Actor}
import akka.testkit.{TestKit, TestActorRef}
import akkaviz.events.types._
import org.scalatest.{FunSuiteLike, Matchers}

class LightSnapshotTest() extends TestKit(ActorSystem("SnapshotTests")) with FunSuiteLike with Matchers with Helpers {

  val firstRef = TestActorRef[SomeActor](new SomeActor, "first")
  val secondRef = TestActorRef[SomeActor](new SomeActor, "second")

  test("should include actors receiving messages as live") {
    val events = Seq(
      ReceivedWithId(1, firstRef, secondRef, "sup", true),
      ReceivedWithId(2, secondRef, firstRef, "sup", true)
    )

    val snapshot = snapshotOf(events)
    snapshot.liveActors should contain allOf (firstRef.path.toString, secondRef.path.toString)
  }

  test("should contain dead actors") {
    val events = Seq(
      ReceivedWithId(1, firstRef, secondRef, "sup", true),
      ReceivedWithId(2, secondRef, firstRef, "sup", true),
      Killed(secondRef)
    )

    val snapshot = snapshotOf(events)
    snapshot.liveActors should contain(actorRefToString(firstRef))
    snapshot.liveActors should not contain actorRefToString(secondRef)
    snapshot.dead should contain(actorRefToString(secondRef))
  }

  test("should contain classes of instantiated actors") {
    val events = Seq(
      Instantiated(firstRef, firstRef.underlyingActor),
      Instantiated(secondRef, secondRef.underlyingActor)
    )
    val snapshot = snapshotOf(events)

    snapshot.classNameFor(firstRef.path.toString) should equal(Some(firstRef.underlyingActor.getClass.getName))
    snapshot.classNameFor(secondRef.path.toString) should equal(Some(secondRef.underlyingActor.getClass.getName))
  }

  test("should include recreated actor as live") {
    val events = Seq(
      Instantiated(firstRef, firstRef.underlyingActor),
      Killed(firstRef),
      Spawned(firstRef)
    )
    val snapshot = snapshotOf(events)
    snapshot.liveActors should contain(actorRefToString(firstRef))
    snapshot.dead should be('empty)
  }

  test("should ignore BackendEvents not pertaining to actor state") {
    import scala.concurrent.duration._
    val events = Seq(
      ActorSystemCreated(system),
      ReportingDisabled,
      ReportingEnabled,
      ThroughputMeasurement(firstRef, 0.0, 0xDEB1L),
      ReceiveDelaySet(2000.seconds)
    )

    snapshotOf(events) should equal(LightSnapshot())
  }

  def snapshotOf(events: Seq[BackendEvent]): LightSnapshot = {
    events.foldLeft(LightSnapshot())(_.update(_))
  }
}

class SomeActor extends Actor {
  override def receive: Receive = { case _ => () }
}
