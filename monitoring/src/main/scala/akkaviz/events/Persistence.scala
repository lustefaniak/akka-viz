package akkaviz.events

import akka.actor.{Actor, ActorSystem}
import akkaviz.serialization.MessageSerialization

object Persistence extends App {
  implicit val system = ActorSystem()

  for (i <- 0 until 1000) {
    EventSystem.report(types.Received(system.deadLetters, system.deadLetters, MessageSerialization.render(("test", 123)), true))
  }

}

