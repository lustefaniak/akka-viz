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
    if (me.system.name != internalSystemName) {
      EventSystem.publish(internal.Received(me.sender(), me.self, msg))
      EventSystem.publish(internal.MailBoxStatus(me.self, me.mailbox.numberOfMessages))
    }
  }

  @Pointcut("execution(akka.actor.ActorCell.new(..)) && this(cell) && args(system, self, props, dispatcher, parent)")
  def actorCellCreation(cell: ActorCell, system: ActorSystemImpl, self: InternalActorRef, props: Props, dispatcher: MessageDispatcher, parent: InternalActorRef): Unit = {}

  @After("actorCellCreation(cell, system, self, props, dispatcher, parent)")
  def captureCellCreation(cell: ActorCell, system: ActorSystemImpl, self: InternalActorRef, props: Props, dispatcher: MessageDispatcher, parent: InternalActorRef): Unit = {
    if (cell.system.name != internalSystemName)
      EventSystem.publish(internal.Spawned(self, parent))
  }

  @Pointcut("execution(* akka.actor.ActorCell.newActor()) && this(cell)")
  def actorCreation(cell: ActorCell): Unit = {}

  @AfterReturning(pointcut = "actorCreation(cell)", returning = "actor")
  def captureActorCreation(cell: ActorCell, actor: Actor): Unit = {
    if (cell.system.name != internalSystemName) {
      val self = cell.self
      EventSystem.publish(internal.Instantiated(self, actor))
      actor match {
        case fsm: akka.actor.FSM[_, _] =>
          fsm.onTransition {
            case (x, y) =>
              EventSystem.publish(internal.FSMTransition(self, x, fsm.stateData, y, fsm.nextStateData))
          }
        case _ => {}
      }


    }
  }

}
