package akka.viz.events

import akka.actor.ActorRef
import akka.viz.events.types._
import Predef.{any2stringadd => _, _}
import scala.language.implicitConversions

case class LightSnapshot(
    liveActors: Set[String] = Set(),
    children: Map[String, Set[String]] = Map(),
    receivedFrom: Set[(String, String)] = Set()
) {

  implicit def ref2String(r: ActorRef): String = r.path.toSerializationFormat
  implicit def refPair2StringPair(pair: (ActorRef, ActorRef)): (String, String) = (ref2String(pair._1), ref2String(pair._2))

  def dead: Set[String] = {
    liveActors diff (children.values.flatten ++ receivedFrom.flatMap(p => Seq(p._1, p._2))).toSet
  }

  def update(ev: BackendEvent): LightSnapshot = ev match {
    case ReceivedWithId(_, from, to, _) =>
      val live = liveActors ++ Seq[String](from, to)
      val recv = receivedFrom + (from -> to)
      copy(liveActors = live, receivedFrom = recv)
    case Spawned(ref, parent) =>
      val live = liveActors + ref
      val childr = children.updated(parent, children.getOrElse(parent, Set()) + ref)
      copy(liveActors = live, children = childr)
    case Killed(ref) =>
      copy(liveActors = liveActors - ref)
    case CurrentActorState(ref, _) =>
      copy(liveActors = liveActors + ref)
    case Instantiated(ref, _) =>
      copy(liveActors = liveActors + ref)
    case other =>
      this
  }
}
