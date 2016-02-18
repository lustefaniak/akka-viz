package roulette

import akka.actor._

import scala.util.Random

class Player extends Actor with ActorLogging {

  var nextGuy: ActorRef = _

  context.become(playerBehaviour)

  override def receive = {
    case _ => ???
  }

  def playerBehaviour: Receive = {
    case Next(ref) =>
      nextGuy = ref

    case Revolver(0) =>
      Thread.sleep(2000)
      log.info("BANG!")
      if (sender() != nextGuy) {
        sender() ! Next(nextGuy)
        nextGuy.tell(Revolver(Random.nextInt(6)), sender())
      }
      self ! Kill

    case Revolver(x) =>
      Thread.sleep(2000)
      log.info("CLICK")
      nextGuy ! Revolver(x - 1)
      nextGuy ! "Unhandled message"
  }
}
