package akka.viz

import akka.actor.ActorRef
import akka.util.Timeout
import akkaviz.config.Config
import org.aspectj.lang.annotation._

import scala.concurrent.{Future, Promise}

@Aspect
class AskInstrumentation {

  private val internalSystemName = Config.internalSystemName

  @Pointcut("execution (* akka.pattern..*.internalAsk$extension(..)) && args(recipient, msg, timeout, sender)")
  def internalAskPointcut(recipient: ActorRef,
                          msg: Any,
                          timeout: Timeout,
                          sender: ActorRef): Unit = {}

  @AfterReturning(pointcut = "internalAskPointcut(recipient, msg, timeout, sender)", returning = "future")
  def afterInternalAsk(future: Future[Any],
                       recipient: ActorRef,
                       msg: Any,
                       timeout: Timeout,
                       sender: ActorRef): Unit = {
    if (!isSystemActor(recipient)) {
      publishQuestion(Option(sender), recipient, msg)
      scheduleAnswer(future)
    }
  }

  private def isSystemActor(ref: ActorRef) =
    ref.path.address.system == internalSystemName

  def publishQuestion(from: Option[ActorRef], to: ActorRef, msg: Any) = {
    println("~~~~~~~~ ASK ~~~~~~~~")
    println("FROM: " + from.map(_.path.toSerializationFormat).getOrElse("noSender"))
    println("TO: " + to.path.toSerializationFormat)
    println("MSG: " + msg)
    println("~~~~~~~~~~~~~~~~~~~~~")
  }

  def scheduleAnswer(answerFuture: Future[Any]) = {
    implicit val ec = scala.concurrent.ExecutionContext.global
    answerFuture.onSuccess { case msg =>
      println("REPLIED: " + msg)
    }
    answerFuture.onFailure { case ex =>
      println("EXCEPTION THROWN: " + ex)
    }
  }
}
