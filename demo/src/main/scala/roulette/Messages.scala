package roulette

import akka.actor.ActorRef

case class Revolver(x: Int) {
  require(x >= 0)
}
case class Next(actorRef: ActorRef)
