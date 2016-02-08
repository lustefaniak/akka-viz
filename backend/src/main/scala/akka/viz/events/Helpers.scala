package akka.viz.events

import akka.actor.ActorRef

trait Helpers {
  implicit def actorRefToString(ar: ActorRef): String = {
    val path = ar.path.toSerializationFormat
    val idPos = path.indexOf('#')
    if (idPos >= 0) path.substring(0, idPos) else path
  }
}

object Helpers extends Helpers
