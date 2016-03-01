package akkaviz

import java.nio.ByteBuffer

import scala.collection.immutable.{List, Set}
import scala.concurrent.duration.{Duration, FiniteDuration}

package object protocol {
  type IsoTs = String

  sealed trait ApiServerMessage

  case class Received(eventId: Long, sender: String, receiver: String, payloadClass: String, payload: Option[String], handled: Boolean) extends ApiServerMessage

  case class AvailableClasses(availableClasses: List[String]) extends ApiServerMessage

  case class Spawned(ref: String) extends ApiServerMessage

  case class ActorSystemCreated(systemName: String) extends ApiServerMessage

  case class Instantiated(ref: String, clazz: String) extends ApiServerMessage

  case class MailboxStatus(owner: String, size: Int) extends ApiServerMessage

  case class FSMTransition(
    ref: String,
    currentState: String,
    currentStateClass: String,
    currentData: String,
    currentDataClass: String,
    nextState: String,
    nextStateClass: String,
    nextData: String,
    nextDataClass: String
  ) extends ApiServerMessage

  case class CurrentActorState(ref: String, state: String) extends ApiServerMessage

  case class ReceiveDelaySet(current: Duration) extends ApiServerMessage

  case class Killed(ref: String) extends ApiServerMessage

  case class ActorFailure(
    actorRef: String,
    cause: String,
    decision: String,
    ts: IsoTs
  ) extends ApiServerMessage

  case object ReportingEnabled extends ApiServerMessage

  case object ReportingDisabled extends ApiServerMessage

  case object Ping extends ApiServerMessage

  case class SnapshotAvailable(
    live: List[String],
    dead: List[String],
    receivedFrom: Set[(String, String)]
  ) extends ApiServerMessage

  sealed trait ApiClientMessage

  case class SetAllowedMessages(allowedClasses: Set[String]) extends ApiClientMessage

  case class SetReceiveDelay(duration: FiniteDuration) extends ApiClientMessage

  case class SetEnabled(isEnabled: Boolean) extends ApiClientMessage

  case class ObserveActors(actors: Set[String]) extends ApiClientMessage

  object IO {

    import boopickle.Default._

    implicit val serverPickler: Pickler[ApiServerMessage] = generatePickler[ApiServerMessage]
    implicit val clientPickler: Pickler[ApiClientMessage] = generatePickler[ApiClientMessage]

    def readServer(bytes: ByteBuffer): ApiServerMessage = {
      Unpickle[ApiServerMessage].fromBytes(bytes)
    }

    def readClient(bytes: ByteBuffer): ApiClientMessage = {
      Unpickle[ApiClientMessage].fromBytes(bytes)
    }

    def write(msg: ApiServerMessage): ByteBuffer = {
      Pickle.intoBytes(msg)
    }

    def write(msg: ApiClientMessage): ByteBuffer = {
      Pickle.intoBytes(msg)
    }

  }

}

