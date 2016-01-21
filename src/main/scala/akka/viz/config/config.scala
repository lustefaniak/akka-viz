package akka.viz.config

import com.typesafe.config.{Config => C}
import com.wacai.config.annotation._

@conf
private[viz] trait akkaviz {

  val internalSystemName = "akka-viz"

  val interface = "127.0.0.1"
  val port = 8888
  val eventsToReply = Int.MaxValue
  val bufferSize: Int = 10000
}

case object Config extends akkaviz
