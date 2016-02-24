package akkaviz.events

import akka.actor.ActorSystem

import scala.ref.WeakReference

object ActorSystems {

  private val systemReferences = scala.collection.mutable.Map[String, WeakReference[ActorSystem]]()

  def systems: scala.collection.immutable.Map[String, ActorSystem] = systemReferences.flatMap {
    case (name, ref) => ref.get.map {
      system => name -> system
    }
  }.toMap

  def registerSystem(system: ActorSystem): Unit = {
    systemReferences.update(system.name, WeakReference(system))
  }

}
