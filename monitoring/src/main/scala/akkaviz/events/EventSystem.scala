package akkaviz.events

import akka.actor._
import akka.util.Timeout
import akkaviz.config.Config
import akkaviz.events.types._
import akkaviz.persistence.EventPersistorActor

import scala.concurrent.duration._

object EventSystem {

  private[this] implicit val timeout = Timeout(100.millis)
  private[this] implicit val system = ActorSystem(Config.internalSystemName)

  private[this] val publisher = system.actorOf(Props(classOf[EventPublisherActor]).withDispatcher(
    "control-aware-dispatcher"
  ), "publisher")

  if (Config.enableArchive) {
    system.actorOf(Props(classOf[EventPersistorActor], publisher), "persistor")
  }

  private[this] val globalSettings = system.actorOf(Props(classOf[GlobalSettingsActor]), "global-settings")
  private[this] val autoStartReporting = Config.autoStartReporting

  globalSettings ! publisher

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

  @volatile
  private[this] var _receiveDelay: FiniteDuration = 0.millis
  def receiveDelay: FiniteDuration = _receiveDelay
  def setReceiveDelay(fd: FiniteDuration): Unit = {
    _receiveDelay = fd
    publish(ReceiveDelaySet(fd))
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