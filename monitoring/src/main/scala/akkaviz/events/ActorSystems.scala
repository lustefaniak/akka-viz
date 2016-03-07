package akkaviz.events

import akka.actor.{ActorPath, ActorRef, ActorSystem}
import akka.viz.ActorCellInstrumentation

import scala.ref.WeakReference
import scala.util.Try

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

  def tell(path: String, message: Any): Unit = {
    Try {
      val actorPath = ActorPath.fromString(path)
      systems.get(actorPath.address.system).foreach {
        system =>
          system.actorSelection(actorPath).tell(message, ActorRef.noSender)
      }
    }
  }

}
