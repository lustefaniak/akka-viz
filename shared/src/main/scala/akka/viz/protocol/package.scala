package akka.viz

package object protocol {

  sealed trait ApiServerMessage

  case class Received(eventId:Long, sender: String, receiver: String, payloadClass:String, payload: Option[String]) extends ApiServerMessage

  case class AvailableClasses(availableClasses: List[String]) extends ApiServerMessage

  sealed trait ApiClientMessage

  case class SetAllowedMessages(allowedClasses: List[String]) extends ApiClientMessage

}
