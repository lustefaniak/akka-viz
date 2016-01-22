package akka.viz.demos.postoffice

import java.time.LocalDateTime

import akka.actor._

import scala.util.Random

class PostOfficeActor(val postOffice: PostOffice) extends Actor with ActorLogging {
  import PostOffice._

  val myClient = context.actorOf(Props(classOf[PostOfficeClientActor]), "client")
  myClient ! postOffice.city

  override def receive: Receive = {
    case p @ Parcel(src, dest, weight) if src == postOffice.city =>
      Thread.sleep(randomDelay)
      if (weight > WeightLimit)
        sender() ! Rejected(LocalDateTime.now(), p)
      else {
        sender() ! Pickup(LocalDateTime.now(), p)
        nextOffice(route(src -> dest)) ! p
      }

    case p @ Parcel(src, dest, _) if dest == postOffice.city =>

      myClient ! Delivery(LocalDateTime.now(), p)

    case p @ Parcel(_, dest, _) =>

      Thread.sleep(randomDelay)
      if(!lostPackage) nextOffice(route(postOffice.city -> dest)) ! p
  }


  def nextOffice(route: List[City]): ActorSelection = {
    val nextCity = route.dropWhile(_ != postOffice.city).drop(1).head

    val selection: ActorSelection = context.system.actorSelection(s"akka://post-office/user/$nextCity")
    selection
  }


  def lostPackage = Random.nextGaussian() < 0.002
}

class PostOfficeClientActor extends Actor with ActorLogging {
  import PostOffice._

  import scala.concurrent.duration._

  var city: Option[City] = None

  override def receive: Actor.Receive = {

    case c: City =>
      city = Some(c)
      sendPackage
      context.become(packageReply)
  }

  def packageReply: Actor.Receive = {
    case Pickup(_, p) =>
      log.debug(s"Sent parcel $p")

    case Rejected(_, p) =>
      log.debug(s"$p rejected, trying again")
      sender() ! p.copy(weight = p.weight - 0.02)

    case d : Delivery =>
      log.debug(s"received $d")
      sendPackage
  }

  def sendPackage = {
   import context.dispatcher
    context.system.scheduler.scheduleOnce(randomDelay.milliseconds,
      context.parent, Parcel(city.get, Random.shuffle(Cities.filterNot(_ == city)).head, Random.nextDouble() * (WeightLimit + 0.10)))
  }


}