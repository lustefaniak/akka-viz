package akkaviz.akka

import akka.actor.ActorRef
import akkaviz.events.types.EventActorRef

trait Helpers {
  implicit def actorRefToString(ar: ActorRef): String = {
    val path = ar.path.toSerializationFormat
    val idPos = path.indexOf('#')
    if (idPos >= 0) path.substring(0, idPos) else path
  }

  implicit def actorRefToEventActorRef(ar: ActorRef): EventActorRef = {
    val path = ar.path.toSerializationFormat
    val idPos = path.indexOf('#')
    EventActorRef(if (idPos >= 0) path.substring(0, idPos) else path)
  }

  implicit class IsUserActorRef(underlying: ActorRef) {
    def isUserActor: Boolean = {
      val elems = underlying.path.elements
      elems.size > 1 && elems.exists(_ == "user")
    }
  }

}

object Helpers extends Helpers
