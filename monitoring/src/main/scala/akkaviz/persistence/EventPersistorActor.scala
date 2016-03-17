package akkaviz.persistence

import akka.actor.{ActorRef, Actor, ActorLogging}
import akkaviz.events.EventPublisherActor.Subscribe
import akkaviz.events.{FilteringRule, Helpers}
import akkaviz.events.Helpers.actorRefToString
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

  override def receive: Receive = {
    case DoInsert =>
      doInsert()

    case r: ReceivedWithId if FilteringRule.isUserActor(r.actorRef) && FilteringRule.isUserActor(r.sender) =>
      val msg = MessageSerialization.render(r.message)
      val id = UUIDs.timeBased()
      val time = System.currentTimeMillis()
      val records = List(
        ReceivedRecord(id, time, actorRefToString(r.sender), To, actorRefToString(r.actorRef), msg),
        ReceivedRecord(id, time, actorRefToString(r.actorRef), From, actorRefToString(r.sender), msg)
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

