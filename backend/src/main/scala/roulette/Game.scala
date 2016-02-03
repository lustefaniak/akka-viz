package roulette

import akka.actor.{Props, ActorSystem}

import scala.util.Random

class Game(playersNo: Int) {
  def run(system: ActorSystem): Unit = {
    val players = Vector.fill(playersNo)(system.actorOf(Props[Player]))

    for (x <- 0 until playersNo) {
      players(x).tell(Next(players((x + 1) % playersNo)), players(x))
    }

    val firstGuyId = Random.nextInt(playersNo)
    val previousGuyId = (firstGuyId - 1 + playersNo) % playersNo
    val firstGuy = players(firstGuyId)
    val previousGuy = players(previousGuyId)

    firstGuy.tell(Revolver(Random.nextInt(6)), previousGuy)
  }
}
