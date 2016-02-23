package ama

import akka.actor.{Props, ActorSystem, Actor}
import akka.pattern._
import akka.util.Timeout
import scala.concurrent.duration._

object AskDemo {

  def run(system: ActorSystem): Unit = {
    val amaActor = system.actorOf(Props[AmaActor])
    implicit val timeout = Timeout(2 seconds)
    amaActor ? 42
    amaActor.ask("Hello")(timeout, Actor.noSender)
    amaActor ? Nil
  }

  class AmaActor extends Actor {
    override def receive: Receive = {
      case _: Int     => sender ! "That's an int"
      case _: String  => sender ! "That's a string"
      case _          => // no answer - to trigger timeout
    }
  }
}
