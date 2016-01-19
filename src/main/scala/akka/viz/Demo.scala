package akka.viz

import akka.actor._

import scala.concurrent.Await
import scala.concurrent.duration._

class Ping(pongRef: ActorRef) extends Actor {
  override def receive: Receive = {
    case "ping" => {
      println(s"pong to $pongRef")
      Thread.sleep(500)
      pongRef ! "pong"
    }
  }
}

class Pong extends Actor {
  override def receive: Actor.Receive = {
    case "pong" => {
      println(s"ping to ${sender()}")
      Thread.sleep(500)
      sender() ! "ping"
    }
  }
}

object Demo extends App {

  val system = ActorSystem()

  val pongRef = system.actorOf(Props(classOf[Pong]), "ponger")
  val pingRef = system.actorOf(Props(classOf[Ping], pongRef), "pinger")
  pingRef ! "ping"

  Thread.sleep(5000)

  Await.result(system.terminate(), 5.minutes)
  sys.exit(0)

}
