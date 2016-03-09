package akkaviz.events

import akka.actor._
import akka.pattern._
import akka.util.Timeout
import akkaviz.config.Config
import akkaviz.events.types._

import scala.concurrent.Await
import scala.concurrent.duration._

object EventSystem {

  private[this] implicit val timeout = Timeout(100.millis)
  private[this] implicit val system = ActorSystem(Config.internalSystemName)

  private[this] val publisher = system.actorOf(Props(classOf[EventPublisherActor]).withDispatcher(
    "control-aware-dispatcher"
  ))
  private[this] val globalSettings = system.actorOf(Props(classOf[GlobalSettingsActor]))
  private[this] val autoStartReporting = Config.autoStartReporting

  globalSettings ! publisher

  def receiveDelay = {
    //FIXME: don't ask every time
    Await.result((globalSettings ? GlobalSettingsActor.GetDelay).mapTo[Duration], timeout.duration)
  }

  private[akkaviz] def receiveDelay_=(d: Duration): Unit = {
    globalSettings ! d
  }

  private[this] def publish(event: InternalEvent): Unit = {
    publisher ! event
  }

  @volatile
  private[this] var _isEnabled: Boolean = false

  def isEnabled() = _isEnabled

  def setEnabled(enabled: Boolean) = {
    _isEnabled = enabled
    publish(if (enabled) ReportingEnabled else ReportingDisabled)
  }

  if (autoStartReporting) setEnabled(true)

  @inline
  def report(event: => InternalEvent): Unit = {
    if (isEnabled) {
      publish(event)
    }
  }

  def subscribe(subscriber: ActorRef): Unit = {
    publisher.tell(EventPublisherActor.Subscribe, subscriber)
  }

}