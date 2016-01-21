package akka.viz.aspects

import akka.actor.{ActorCell, ActorRef, DeadLetterActorRef}
import akka.viz.config.Config
import akka.viz.events.{Received, EventSystem}
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation._

@Aspect
class ActorCellInstrumentation {

  private val internalSystemName = Config.internalSystemName

  @Pointcut(value = "execution (* akka.actor.ActorCell.receiveMessage(..)) && args(msg)", argNames = "msg")
  def receiveMessagePointcut(msg: Any): Unit = {}

  @Before(value = "receiveMessagePointcut(msg) && this(me)", argNames = "jp,msg,me")
  def message(jp: JoinPoint, msg: Any, me: ActorCell) {
    if (me.system.name != internalSystemName)
      EventSystem.publish(Received(me.sender(), me.self, msg))
  }

}
