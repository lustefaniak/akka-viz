package akka.viz.events

import akka.actor._

object EventFiltering {

  private def isUserActor(actorRef: ActorRef): Boolean = {
    actorRef.path.elements.headOption.map(_ == "user").getOrElse(false)
  }

  def isAllowed(event: Event): Boolean = {
    event match {
      case Received(sender, receiver, message) =>
        (isUserActor(sender) || isUserActor(receiver)) && isMessageAllowed(message)
    }
  }

  private def isMessageAllowed(msg: Any): Boolean = {

    true

  }

}
