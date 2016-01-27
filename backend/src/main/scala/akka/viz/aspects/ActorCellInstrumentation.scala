package akka.viz.aspects

import akka.actor._
import akka.dispatch.MessageDispatcher
import akka.viz.config.Config
import akka.viz.events.{EventSystem, internal}
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
      EventSystem.publish(internal.Received(me.sender(), me.self, msg))
  }

  @Pointcut("execution(akka.actor.ActorCell.new(..)) && this(cell) && args(system, self, props, dispatcher, parent)")
  def actorCellCreation(cell: ActorCell, system: ActorSystemImpl, self: InternalActorRef, props: Props, dispatcher: MessageDispatcher, parent: InternalActorRef): Unit = {}

  @After("actorCellCreation(cell, system, self, props, dispatcher, parent)")
  def captureCreation(cell: ActorCell, system: ActorSystemImpl, self: InternalActorRef, props: Props, dispatcher: MessageDispatcher, parent: InternalActorRef): Unit = {
    if (cell.system.name != internalSystemName)
      EventSystem.publish(internal.Spawned(self, parent))
  }

}
