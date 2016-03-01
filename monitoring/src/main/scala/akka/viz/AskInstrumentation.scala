package akka.viz

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorRef
import akka.util.Timeout
import akkaviz.config.Config
import akkaviz.events.EventSystem
import akkaviz.events.types.{Answer, AnswerFailed, Question}
import org.aspectj.lang.annotation._

import scala.concurrent.Future
import scala.util.{Failure, Success}

@Aspect
class AskInstrumentation {

  private val askCounter: AtomicLong = new AtomicLong(0)
  private val internalSystemName = Config.internalSystemName

  @Pointcut("execution (* akka.pattern..*.internalAsk$extension(..)) && args(recipient, msg, timeout, sender)")
  def internalAskPointcut(
    recipient: ActorRef,
    msg: Any,
    timeout: Timeout,
    sender: ActorRef
  ): Unit = {}

  @AfterReturning(pointcut = "internalAskPointcut(recipient, msg, timeout, sender)", returning = "future")
  def afterInternalAsk(
    future: Future[Any],
    recipient: ActorRef,
    msg: Any,
    timeout: Timeout,
    sender: ActorRef
  ): Unit = {
    if (!isSystemActor(recipient)) {
      val questionId = askCounter.incrementAndGet()
      publishQuestion(questionId, Option(sender), recipient, msg)
      scheduleAnswer(questionId, future)
    }
  }

  private def isSystemActor(ref: ActorRef) =
    ref.path.address.system == internalSystemName

  def publishQuestion(
    questionId: Long,
    from: Option[ActorRef],
    to: ActorRef,
    msg: Any
  ): Unit = {
    EventSystem.report(Question(questionId, from, to, msg))
  }

  def scheduleAnswer(questionId: Long, answerFuture: Future[Any]): Unit = {
    implicit val ec = scala.concurrent.ExecutionContext.global
    answerFuture.onComplete {
      case Success(msg) => EventSystem.report(Answer(questionId, msg))
      case Failure(ex)  => EventSystem.report(AnswerFailed(questionId, ex))
    }
  }
}
