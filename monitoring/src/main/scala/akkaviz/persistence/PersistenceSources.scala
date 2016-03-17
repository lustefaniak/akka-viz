package akkaviz.persistence

import akka.stream.scaladsl.Source
import io.getquill._
import io.getquill.naming.SnakeCase
import org.reactivestreams.Publisher
import monifu.concurrent.Implicits.globalScheduler

import scala.util.control.NonFatal

object PersistenceSources {

  private[this] lazy val db = source(new CassandraStreamSourceConfig[SnakeCase]("akkaviz.cassandra"))

  def of(ref: String): Source[ReceivedRecord, _] = {
    try {
      Source.fromPublisher(db.run(Queries.getAllFor)(ref))
    } catch {
      case NonFatal(e) =>
        Source.failed(e)
    }
  }

  def between(ref: String, ref2: String): Source[ReceivedRecord, _] = {
    try {
      Source.fromPublisher(db.run(Queries.getBetween)(ref, ref2, To))
    } catch {
      case NonFatal(e) =>
        Source.failed(e)
    }
  }

}
