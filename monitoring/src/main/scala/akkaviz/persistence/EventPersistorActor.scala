package akkaviz.persistence

import akka.actor.{ActorRef, Actor, ActorLogging}
import akkaviz.events.EventPublisherActor.Subscribe
import akkaviz.events.{FilteringRule, Helpers}
import akkaviz.events.types._
import akkaviz.serialization.MessageSerialization
import com.datastax.driver.core.utils.UUIDs
import io.getquill._
import io.getquill.naming._

import scala.concurrent.duration._

class EventPersistorActor(publisherRef: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher

  private[this] var queue = List[ReceivedRecord]()

  private[this] val maxItemsInQueue = 20

  override def preStart(): Unit = {
    super.preStart()
    context.system.scheduler.schedule(30.seconds, 30.seconds, self, DoInsert)
    publisherRef ! Subscribe
  }

  override def receive = {
    case DoInsert =>
      doInsert()

    case r: ReceivedWithId if FilteringRule.isUserActor(r.actorRef) && FilteringRule.isUserActor(r.sender) =>
      val records = List(
        ReceivedRecord(UUIDs.timeBased(), Helpers.actorRefToString(r.sender), To, Helpers.actorRefToString(r.actorRef), MessageSerialization.render(r.message)),
        ReceivedRecord(UUIDs.timeBased(), Helpers.actorRefToString(r.actorRef), From, Helpers.actorRefToString(r.sender), MessageSerialization.render(r.message))
      )
      queue ++= records
      if (queue.size >= maxItemsInQueue) {
        doInsert()
      }

    case _ => {}

  }

  private[this] case object DoInsert

  private[this] def doInsert(): Unit = {
    if (queue.nonEmpty) {
      db.run(query[ReceivedRecord].insert)(queue)
      queue = List()
    }
  }

  private[this] val db = source(new CassandraSyncSourceConfig[SnakeCase]("akkaviz.cassandra"))

}

