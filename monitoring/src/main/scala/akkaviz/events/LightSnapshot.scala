package akkaviz.events

import akka.actor.ActorRef
import akkaviz.events.Helpers._
import akkaviz.events.LightSnapshot.{ClassName, ActorRefEquiv}
import akkaviz.events.types._

import scala.Predef.{any2stringadd => _, _}
import scala.language.implicitConversions

case class LightSnapshot(
    liveActors: Set[ActorRefEquiv] = Set(),
    receivedFrom: Set[(ActorRefEquiv, ActorRefEquiv)] = Set(),
    classes: Map[ActorRefEquiv, ClassName] = Map()
) {

  def classNameFor(ref: ActorRefEquiv): Option[ClassName] = classes.get(ref)

  implicit def refPair2StringPair(pair: (ActorRef, ActorRef)): (String, String) = {
    val (actor1, actor2) = pair
    (actorRefToString(actor1), actorRefToString(actor2))
  }

  def dead: Set[String] = {
    liveActors diff (receivedFrom.flatMap(p => Seq(p._1, p._2))).toSet
  }

  def update(ev: BackendEvent): LightSnapshot = ev match {
    case ReceivedWithId(_, from, to, _, _) =>
      val live: Set[String] = liveActors ++ Set[ActorRef](from, to).filter(_.isUserActor).map(actorRefToString)
      val recv = receivedFrom + (from -> to)
      copy(liveActors = live, receivedFrom = recv)
    case Spawned(ref) =>
      if (ref.isUserActor) {
        copy(liveActors = liveActors + ref)
      } else {
        this
      }
    case Killed(ref) if ref.isUserActor =>
      copy(liveActors = liveActors - ref)
    case CurrentActorState(ref, _) if ref.isUserActor =>
      copy(liveActors = liveActors + ref)
    case Instantiated(ref, actor) if ref.isUserActor =>
      copy(liveActors = liveActors + ref, classes = classes.updated(ref, actor.getClass.getName))
    case CurrentActorState(ref, actor) if ref.isUserActor =>
      copy(classes = classes.updated(ref, actor.getClass.getName))
    case other =>
      this
  }
}

object LightSnapshot {
  type ActorRefEquiv = String
  type ClassName = String

}
