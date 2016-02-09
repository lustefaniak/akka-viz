package roulette

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.util.Random

class Player extends Actor with ActorLogging {

  var nextGuy: ActorRef = _

  override def receive = {

    case Next(ref) =>
      nextGuy = ref

    case Revolver(0) =>
      Thread.sleep(2000)
      log.info("BANG!")
      if (sender() != nextGuy) {
        sender() ! Next(nextGuy)
        nextGuy.tell(Revolver(Random.nextInt(6)), sender())
      }

      context.stop(self)

    case Revolver(x) =>
      Thread.sleep(2000)
      log.info("CLICK")
      nextGuy ! Revolver(x - 1)
  }
}
