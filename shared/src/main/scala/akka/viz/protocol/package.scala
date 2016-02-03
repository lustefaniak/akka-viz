package akka.viz

package object protocol {

  sealed trait ApiServerMessage

  case class Received(eventId: Long, sender: String, receiver: String, payloadClass: String, payload: Option[String]) extends ApiServerMessage

  case class AvailableClasses(availableClasses: List[String]) extends ApiServerMessage

  case class Spawned(ref: String, parent: String) extends ApiServerMessage

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

  sealed trait ApiClientMessage

  case class SetAllowedMessages(allowedClasses: List[String]) extends ApiClientMessage

}
