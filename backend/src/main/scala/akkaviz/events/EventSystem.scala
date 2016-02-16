package akkaviz.events

import akka.actor._
import akka.pattern._
import akka.util.Timeout
import akkaviz.config.Config
import akkaviz.events.types._

import scala.concurrent.Await
import scala.concurrent.duration._

object EventSystem {

  private implicit val timeout = Timeout(100.millis)
  private implicit val system = ActorSystem(Config.internalSystemName)

  private val publisher = system.actorOf(Props(classOf[EventPublisherActor]))
  private val globalSettings = system.actorOf(Props(classOf[GlobalSettingsActor]))
  private val autoStartReporting = Config.autoStartReporting

  globalSettings ! publisher

  def receiveDelay = {
    //FIXME: it can't be retrieved every time
    Await.result((globalSettings ? GlobalSettingsActor.GetDelay).mapTo[Duration], timeout.duration)
  }

  private[akkaviz] def receiveDelay_=(d: Duration): Unit = {
    globalSettings ! d
  }

  private def publish(event: InternalEvent): Unit = {
    publisher ! event
  }

  @volatile
  private var _isEnabled: Boolean = false

  def isEnabled() = _isEnabled

  def setEnabled(enabled: Boolean) = {
    println(s"setEnabled: ${enabled}")
    _isEnabled = enabled
    publish(if (enabled) ReportingEnabled else ReportingDisabled)
  }

  def setReceiveDelay(d: Duration): Unit = {
    receiveDelay_=(d)
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