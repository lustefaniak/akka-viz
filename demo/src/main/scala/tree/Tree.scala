package tree

import akka.actor.{ActorSystem, Props, Actor}

object TreeDemo {

  class TreeActor extends Actor {

    import TreeActor._

    override def receive: Actor.Receive = {
      case AreYouOk => sender() ! YesIAm

      case YesIAm => {
        println(s"${sender()} is ok, good to know")
      }

      case name: String =>
        println(s"Will create new node '${name}'")
        val createdRef = context.actorOf(makeItGrowProps, name)
        createdRef ! AreYouOk
        sender ! createdRef
    }
  }

  object TreeActor {

    object AreYouOk

    object YesIAm

  }

  private[this] val makeItGrowProps = Props[TreeActor]

  def run(system: ActorSystem): Unit = {

    val tree = system.actorOf(makeItGrowProps, "tree")
    tree ! TreeActor.AreYouOk

    /*
  //REPL:
  systems

  val system = systems("tree")

  val treeRef = system.actorSelection("/user/tree")

  treeRef ! "subtree1"

  treeRef ! "subtree2"
  treeRef ! "subtree3"

  val subRef1 = system.actorSelection("/user/tree/subtree1")
  val subRef2 = system.actorSelection("/user/tree/subtree2")
  val subRef3 = system.actorSelection("/user/tree/subtree3")

  subRef1 ! "subtree11"
  subRef1 ! "subtree12"

  subRef2 ! "bad actor name"

  val subRef31f = subRef3 ? "subtree31"
  implicit val timeout:akka.util.Timeout = 20.seconds
  val subRef31f = subRef3 ? "subtree31"

  val subRef31tf = subRef31f.mapTo[ActorRef]

  scala.concurrent.Await.result(subRef31tf, 1.second)


  */

  }
}
