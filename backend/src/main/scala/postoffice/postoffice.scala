package postoffice

import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import akka.viz.serialization.{AkkaVizSerializer, SerializationContext}
import upickle.Js
import upickle.Js.Value

import scala.util.Random

case class Parcel(source: City, destination: City, weight: Weight)

case class PostOffice(city: City)

sealed trait ParcelAction

case class Pickup(date: LocalDateTime, parcel: Parcel) extends ParcelAction

case class Delivery(date: LocalDateTime, parcel: Parcel) extends ParcelAction

case class Rejected(date: LocalDateTime, parcel: Parcel) extends ParcelAction

sealed trait City

case object Wroclaw extends City

case object Lodz extends City

case object Warszawa extends City

case object Poznan extends City

case object Krakow extends City

case object GorzowWlkp extends City

case object Berlin extends City

object PostOfficeSerializer extends AkkaVizSerializer {
  override def canSerialize(obj: scala.Any): Boolean = obj match {
    case c: City => true
    case _       => false
  }

  override def serialize(obj: scala.Any, context: SerializationContext): Value = obj match {
    case c: City => Js.Str(c.getClass.getName.split('.').last.stripSuffix("$"))
  }
}

object PostOffice {
  val ParcelRoutes = Map[(City, City), List[City]](
    (Wroclaw -> Warszawa) -> (Wroclaw :: Lodz :: Warszawa :: Nil),
    (Krakow -> Warszawa) -> (Krakow :: Lodz :: Warszawa :: Nil),
    (Berlin -> Warszawa) -> (Berlin :: GorzowWlkp :: Poznan :: Warszawa :: Nil),
    (GorzowWlkp -> Wroclaw) -> (GorzowWlkp :: Poznan :: Wroclaw :: Nil)
  )

  def route: ((City, City)) => List[City] = {
    case (src, dest) =>
      ParcelRoutes
        .get(src -> dest)
        .orElse(ParcelRoutes.get(dest -> src).map(_.reverse))
        .getOrElse(src :: dest :: Nil)
  }

  val WeightLimit = 5.00

  val DelayRange = 1150 to 5500

  def randomDelay = Random.nextInt(DelayRange.max - DelayRange.min) + DelayRange.min

  val Cities: Vector[City] = Vector(
    Wroclaw,
    Lodz,
    Warszawa,
    Poznan,
    Krakow,
    GorzowWlkp,
    Berlin
  )

  def run(implicit system: ActorSystem) = {
    for (city <- PostOffice.Cities) {
      system.actorOf(Props(classOf[PostOfficeActor], PostOffice(city)), city.toString)
    }
  }

}