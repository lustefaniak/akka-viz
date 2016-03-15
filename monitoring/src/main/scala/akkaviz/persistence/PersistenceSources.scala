package akkaviz.persistence

import io.getquill._
import io.getquill.naming.SnakeCase
import org.reactivestreams.Publisher
import monifu.concurrent.Implicits.globalScheduler

object PersistenceSources {

  private[this] val db = source(new CassandraStreamSourceConfig[SnakeCase]("akkaviz.db.cassandra"))

  def of(ref: String): Publisher[ReceivedRecord] = {
    db.run(Queries.getAllFor)(ref).toReactive
  }

}
