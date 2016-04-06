package restartDemo

import akka.actor._

import scala.util.Random

object RestartDemo {
  def run(system: ActorSystem): Unit = {
    system.actorOf(Props[DangerZoneParent], "dangerZoneParent") ! DoIt
  }
}

class DangerZoneParent extends Actor {

  override def supervisorStrategy = OneForOneStrategy(){
    case e: Exception => SupervisorStrategy.Restart
  }

  override def receive: Receive = {
    case DoIt => context.actorOf(Props[DangerZoneActor], "dangerZone")
  }
}

class DangerZoneActor extends Actor with ActorLogging {
  import scala.concurrent.duration._
  import context.dispatcher

  var cancellable: Option[Cancellable] = None

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    cancellable.foreach(_.cancel())
    super.preRestart(reason, message)
  }

  override def preStart(): Unit = {
    cancellable = Some(scheduleRideToTheDangerZone)
  }

  override def receive: Receive = {
    case DangerZone =>
      if(Random.nextBoolean()) rideIntoTheDangerZone
  }

  def rideIntoTheDangerZone: Unit = throw new RuntimeException("the danger zone was too dangerous")

  def scheduleRideToTheDangerZone: Cancellable = context.system.scheduler.schedule(10.seconds , 20.seconds, self, DangerZone)
}

case object DangerZone
case object DoIt