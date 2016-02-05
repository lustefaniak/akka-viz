package akka.viz.events

import akka.actor._
import akka.pattern._
import akka.util.Timeout
import akka.viz.config.Config
import akka.viz.events.api.{NoReporting, PublishingApi, ReportingApi}
import akka.viz.events.types._

import scala.concurrent.Await
import scala.concurrent.duration._

object EventSystem {

  implicit val timeout = Timeout(100.millis)
  private implicit val system = ActorSystem(Config.internalSystemName)

  private val publisher = system.actorOf(Props(classOf[EventPublisherActor]))
  private val globalSettings = system.actorOf(Props(classOf[GlobalSettingsActor]))
  private val autoStartReporting = Config.autoStartReporting

  globalSettings ! publisher

  private[akka] def receiveDelay = {
    Await.result((globalSettings ? GlobalSettingsActor.GetDelay).mapTo[Duration], timeout.duration)
  }

  private[akka] def receiveDelay_=(d: Duration): Unit = {
    globalSettings ! d
  }

  private def publish(event: InternalEvent): Unit = {
    publisher ! event
  }

  @volatile
  private var reportingApi: ReportingApi = NoReporting

  private[akka] def isEnabled = !(reportingApi == NoReporting)

  private[akka] def setEnabled(enabled: Boolean) = {
    println(s"setEnabled: ${enabled}")
    reportingApi = if (enabled) PublishingApi(publish) else NoReporting
    publish(if (enabled) ReportingEnabled else ReportingDisabled)
  }

  if (autoStartReporting) setEnabled(true)

  @inline
  def report: ReportingApi = reportingApi

  def subscribe(subscriber: ActorRef): Unit = {
    publisher.tell(EventPublisherActor.Subscribe, subscriber)
  }

}