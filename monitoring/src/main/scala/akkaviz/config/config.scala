package akkaviz.config

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
  val enableArchive = true
  val maxEventsInSnapshot = 200

  val cassandra = new {
    val keyspace = "akkaviz"
    val preparedStatementCacheSize = 100
    val session = new {
      val contactPoint = "127.0.0.1"
    }
  }

}

case object Config extends akkaviz
