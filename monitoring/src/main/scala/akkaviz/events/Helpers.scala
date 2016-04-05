package akkaviz.events

import akka.actor.ActorRef

trait Helpers {
  implicit def actorRefToString(ar: ActorRef): String = ar match {
    case ActorRef.noSender => ""
    case _ =>
      val path = ar.path.toSerializationFormat
      val idPos = path.indexOf('#')
      if (idPos >= 0) path.substring(0, idPos) else path
  }

  implicit class IsUserActorRef(underlying: ActorRef) {
    def isUserActor: Boolean = underlying match {
      case ActorRef.noSender => false
      case _ =>
        val elems = underlying.path.elements
        elems.size > 1 && elems.exists(_ == "user")
    }
  }

}

object Helpers extends Helpers
