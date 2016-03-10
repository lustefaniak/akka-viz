package akka.viz

import akka.actor._
import akkaviz.config.Config
import akkaviz.events.{ActorSystems, EventSystem}
import akkaviz.events.types.ActorSystemCreated
import org.aspectj.lang.annotation._

@Aspect
class ActorSystemInstrumentation {

  private[this] val internalSystemName = Config.internalSystemName

  @Pointcut("execution(akka.actor.ActorSystem.new(..)) && this(system)")
  def actorSystemCreation(system: ActorSystem): Unit = {}

  @After("actorSystemCreation(system)")
  def captureSystemCreation(system: ActorSystem): Unit = {
    if (system.name != internalSystemName) {
      EventSystem.report(ActorSystemCreated(system))
      ActorSystems.registerSystem(system)
    }
  }

}
