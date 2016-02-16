package akkaviz.config

import com.typesafe.config.{Config => C}
import com.wacai.config.annotation._

@conf
private[akkaviz] trait akkaviz {
  val internalSystemName = "akka-viz"
  val interface = "127.0.0.1"
  val port = 8888
  val bufferSize: Int = 10000
  val maxSerializationDepth = 3
  val inspectObjects = false
  val autoStartReporting = true
}

case object Config extends akkaviz
