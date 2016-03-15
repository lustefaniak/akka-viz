import akka.actor.ActorSystem
import akkaviz.events.LightSnapshot
import akkaviz.serialization.MessageSerialization

import scala.util.Try

object Perf {

  MessageSerialization.preload()
  while (true) {

    Profile.run()

  }

}

object Profile {

  val system = ActorSystem()
  val obj = LightSnapshot()

  def run(): Unit = {
    MessageSerialization.render("test")
    MessageSerialization.render(123)
    MessageSerialization.render(system)
    MessageSerialization.render(obj)
    MessageSerialization.render(None)
    MessageSerialization.render(Some(system))
    MessageSerialization.render(Some("test"))
    MessageSerialization.render(Some(obj))
    MessageSerialization.render(Try(obj))
  }

}