package akka

import akka.actor.SupervisorStrategy.Directive
import akka.actor._
import akka.dispatch.MessageDispatcher
import akkaviz.akka.Helpers
import akkaviz.config.Config
import akkaviz.events.EventSystem
import akkaviz.events.types._
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation._

@Aspect
class ActorCellInstrumentation {

  import Helpers._

  private val internalSystemName = Config.internalSystemName

  @Pointcut(value = "execution (* akka.actor.ActorCell.receiveMessage(..)) && args(msg)", argNames = "msg")
  def receiveMessagePointcut(msg: Any): Unit = {}

  @Before(value = "receiveMessagePointcut(msg) && this(me)", argNames = "jp,msg,me")
  def message(jp: JoinPoint, msg: Any, me: ActorCell) {
    if (me.system.name != internalSystemName) {
      Thread.sleep(EventSystem.receiveDelay.toMillis)
      EventSystem.report(Received(me.sender(), me.self, msg))
      EventSystem.report(MailboxStatus(me.self, me.mailbox.numberOfMessages))
    }
  }

  @After(value = "receiveMessagePointcut(msg) && this(me)", argNames = "jp,msg,me")
  def afterMessage(jp: JoinPoint, msg: Any, me: ActorCell) {
    if (me.system.name != internalSystemName) {
      //FIXME: only if me.self is registered for tracking internal state
      EventSystem.report(CurrentActorState(me.self, me.actor))
    }
  }

  @Pointcut("execution(akka.actor.ActorCell.new(..)) && this(cell) && args(system, self, props, dispatcher, parent)")
  def actorCellCreation(cell: ActorCell, system: ActorSystemImpl, self: InternalActorRef, props: Props, dispatcher: MessageDispatcher, parent: InternalActorRef): Unit = {}

  @After("actorCellCreation(cell, system, self, props, dispatcher, parent)")
  def captureCellCreation(cell: ActorCell, system: ActorSystemImpl, self: InternalActorRef, props: Props, dispatcher: MessageDispatcher, parent: InternalActorRef): Unit = {
    if (cell.system.name != internalSystemName)
      EventSystem.report(Spawned(self, parent))
  }

  @Pointcut("execution(* akka.actor.ActorCell.newActor()) && this(cell)")
  def actorCreation(cell: ActorCell): Unit = {}

  @AfterReturning(pointcut = "actorCreation(cell)", returning = "actor")
  def captureActorCreation(cell: ActorCell, actor: Actor): Unit = {
    if (cell.system.name != internalSystemName) {
      val self = cell.self
      EventSystem.report(Instantiated(self, actor))
      actor match {
        case fsm: akka.actor.FSM[_, _] =>
          //FIXME: unregister?
          fsm.onTransition {
            case (x, y) =>
              EventSystem.report(FSMTransition(self, x, fsm.stateData, y, fsm.nextStateData))
          }
        case _ => {}
      }

    }
  }

  @Pointcut("execution(* akka.actor.Actor.postStop()) && this(actor)")
  def actorTermination(actor: Actor): Unit = {}

  @After("actorTermination(actor)")
  def captureActorTermination(actor: Actor): Unit = {
    if (actor.context.system.name != internalSystemName) {
      EventSystem.report(Killed(actor.self))
    }
  }

  @Pointcut("execution(* akka.actor.SupervisorStrategy.logFailure(..)) && this(strategy) && args(context, child, cause, decision)")
  def handleFailure(strategy: SupervisorStrategy, context: ActorContext, child: ActorRef, cause: Throwable, decision: Directive): Unit = {}

  @After("handleFailure(strategy, context, child, cause, decision)")
  def captureHandleFailure(strategy: SupervisorStrategy, context: ActorContext, child: ActorRef, cause: Throwable, decision: Directive): Unit = {
    if (context.system.name != internalSystemName) {
      EventSystem.report(ActorFailure(child, cause, decision.toString))
    }
  }

}
