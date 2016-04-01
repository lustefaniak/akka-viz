package akkaviz.server

import akka.actor.ActorRef
import akkaviz.events.types._
import akkaviz.events.{ActorRefFilter, AllowedClasses}

trait SubscriptionSession {

  case class SubscriptionSettings(actorEventFilter: ActorRef => Boolean, messageFilter: Any => Boolean) {
    def eventAllowed(event: BackendEvent): Boolean = {
      event match {
        case r: ReceivedWithId      => actorEventFilter(r.actorRef) && messageFilter(r.message)
        case ae: FilteredActorEvent => actorEventFilter(ae.actorRef)
        case _                      => true
      }
    }
  }

  sealed trait ChangeSubscriptionSettings

  case class SetActorEventFilter(actors: Set[String]) extends ChangeSubscriptionSettings

  case class SetAllowedClasses(classes: Set[String]) extends ChangeSubscriptionSettings

  protected def defaultSettings: SubscriptionSettings = SubscriptionSettings(_ => false, _ => false)

  protected def updateSettings: (SubscriptionSettings, ChangeSubscriptionSettings) => SubscriptionSettings = {
    case (settings, op) =>
      op match {
        case SetAllowedClasses(classes) =>
          settings.copy(messageFilter = AllowedClasses(classes))
        case SetActorEventFilter(actors) =>
          settings.copy(actorEventFilter = ActorRefFilter(actors))

      }
  }
}
