package akka.viz.aspects

import akka.actor.{ActorCell, ActorRef, DeadLetterActorRef}
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation._

@Aspect
class ActorCellInstrumentation {

  @Pointcut(value = "execution (* akka.actor.ActorCell.receiveMessage(..)) && args(msg)", argNames = "msg")
  def receiveMessagePointcut(msg: Any): Unit = {

    println("msg")

  }

  private def nicerActorRef(ar: ActorRef): String = {
    if (ar == DeadLetterActorRef) "(NONE)" else ar.path.name
  }


  @Before(value = "receiveMessagePointcut(msg) && this(me)", argNames = "jp,msg,me")
  def message(jp: JoinPoint, msg: Any, me: ActorCell) {
    println(s"Intercepted $msg from ${nicerActorRef(me.sender())} to ${nicerActorRef(me.self)}")

  }

}
