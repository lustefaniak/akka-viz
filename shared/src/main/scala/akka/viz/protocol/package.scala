package akka.viz

package object protocol {

  sealed trait ApiServerMessage

  case class Received(sender: String, receiver: String, message: String) extends ApiServerMessage

  case class AvailableClasses(availableClasses: List[String]) extends ApiServerMessage

  sealed trait ApiClientMessage

  case class SetAllowedMessages(allowedClasses: List[String]) extends ApiClientMessage

}
