package ask

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object AskDemo {

  def run(system: ActorSystem): Unit = {
    implicit val ec = ExecutionContext.global
    val askActor = system.actorOf(Props[AskActor])
    implicit val timeout = Timeout(2 seconds)

    system.scheduler.schedule(0 second, 10 seconds, toRunnable(askActor ? 42))
    system.scheduler.schedule(0 second, 10 seconds, toRunnable(askActor ? Nil))
    system.scheduler.schedule(5 second, 10 seconds, toRunnable(askActor.ask("Hello")(timeout, Actor.noSender)))
  }

  class AskActor extends Actor {
    override def receive: Receive = {
      case _: Int => sender ! "That's an int"
      case _: String => sender ! "That's a string"
      case _ => // no answer - to trigger timeout
    }
  }

  private def toRunnable(thunk: => Any) = new Runnable {
    override def run(): Unit = thunk
  }
}

