import akka.actor.{Actor, ActorSystem, Props}
import fsm.DiningHakkersOnFsm
import postoffice.PostOffice
import roulette.RussianRoulette
import spray.SprayDemo
import tree.TreeDemo

import scala.util.Random

object Main extends App {
  DiningHakkersOnFsm.run(ActorSystem("fsm"))
  PostOffice.run(ActorSystem("postoffice"))
  SprayDemo.run(ActorSystem("spray"))
  TreeDemo.run(ActorSystem("tree"))
  new RussianRoulette(5).run(ActorSystem("russianroulette"))

  val system = ActorSystem("smalldemos")

  val lazyActorProps = Props(new Actor {
    var counter = 0

    override def receive: Receive = {
      case msg =>
        Thread.sleep(Random.nextInt(2000))
        counter += 1
        sender() ! msg
    }
  })

  val lazyActor1 = system.actorOf(lazyActorProps, "lazy1")
  val lazyActor2 = system.actorOf(lazyActorProps, "lazy2")
  for (i <- 0 to 1000) {
    lazyActor1.tell("doit", lazyActor2)
  }

}
