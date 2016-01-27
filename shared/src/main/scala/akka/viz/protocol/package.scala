package akka.viz

package object protocol {

  sealed trait ApiServerMessage

  case class Received(eventId: Long, sender: String, receiver: String, payloadClass: String, payload: Option[String]) extends ApiServerMessage

  case class AvailableClasses(availableClasses: List[String]) extends ApiServerMessage

  case class Spawned(eventId: Long, ref: String, parent: String) extends ApiServerMessage

  case class Instantiated(eventId: Long, ref: String, clazz: String) extends ApiServerMessage

  case class MailboxStatus(eventId: Long, owner: String, size: Int) extends ApiServerMessage

  sealed trait ApiClientMessage

  case class SetAllowedMessages(allowedClasses: List[String]) extends ApiClientMessage

}
