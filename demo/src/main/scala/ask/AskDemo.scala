package ask

import akka.actor.{ActorRef, Actor, ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object AskDemo {

  case class StartAsking(ref: ActorRef)

  implicit val timeout = Timeout(2 seconds)
  implicit val ec = ExecutionContext.global

  def run(system: ActorSystem): Unit = {
    val answeringActor = system.actorOf(Props[AnsweringActor], "answerer")
    val askingActor = system.actorOf(Props[AskingActor], "asker")
    askingActor ! StartAsking(answeringActor)
    system.scheduler.schedule(0 second, 10 seconds, toRunnable(answeringActor ? 42))
  }

  class AnsweringActor extends Actor {
    override def receive: Receive = {
      case _: Int => sender ! "That's an int"
      case _: String => sender ! "That's a string"
      case _ => // no answer - to trigger timeout
    }
  }

  class AskingActor extends Actor {

    def idle = PartialFunction.empty

    override def receive = {
      case StartAsking(ref) =>
        context.system.scheduler.schedule(0 seconds, 10 seconds, toRunnable(ref ? Nil))
        context.system.scheduler.schedule(5 second, 10 seconds, toRunnable(ref.ask("Hello")))
        context.become(idle)
    }
  }

  private[this] def toRunnable(thunk: => Any) = new Runnable {
    override def run(): Unit = thunk
  }
}

