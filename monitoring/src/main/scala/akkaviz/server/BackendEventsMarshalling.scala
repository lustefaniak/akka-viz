package akkaviz.server

import akka.actor.ActorRef
import akka.stream.scaladsl.Flow
import akkaviz.events.Helpers
import akkaviz.events.types._
import akkaviz.protocol
import akkaviz.serialization.MessageSerialization

import scala.collection._

trait BackendEventsMarshalling {

  @inline
  private[this] implicit val actorRefToString: Function1[ActorRef, String] = Helpers.actorRefToString

  def backendEventToProtocolFlow: Flow[BackendEvent, protocol.ApiServerMessage, _] = Flow[BackendEvent].map {
    case ReceivedWithId(eventId, sender, receiver, message, handled) =>
      protocol.Received(eventId, sender, receiver, message.getClass.getName, Some(MessageSerialization.render(message)), handled)

    case AvailableMessageTypes(types) =>
      protocol.AvailableClasses(types.map(_.getName))

    case Spawned(ref) =>
      protocol.Spawned(ref)

    case ActorSystemCreated(system) =>
      protocol.ActorSystemCreated(system.name)

    case Instantiated(ref, clazz) =>
      protocol.Instantiated(ref, clazz.getClass.getName)

    case FSMTransition(ref, currentState, currentData, nextState, nextData) =>
      protocol.FSMTransition(
        ref,
        currentState = MessageSerialization.render(currentState),
        currentStateClass = currentState.getClass.getName,
        currentData = MessageSerialization.render(currentData),
        currentDataClass = currentData.getClass.getName,
        nextState = MessageSerialization.render(nextState),
        nextStateClass = nextState.getClass.getName,
        nextData = MessageSerialization.render(nextData),
        nextDataClass = nextData.getClass.getName
      )

    case CurrentActorState(ref, actor) =>
      protocol.CurrentActorState(ref, MessageSerialization.render(actor))

    case MailboxStatus(owner, size) =>
      protocol.MailboxStatus(owner, size)

    case ReceiveDelaySet(current) =>
      protocol.ReceiveDelaySet(current)

    case Killed(ref) =>
      protocol.Killed(ref)

    case ActorFailure(ref, ex, decision, ts) =>
      protocol.ActorFailure(ref, MessageSerialization.render(ex), decision.toString, ts)

    case Question(id, senderOpt, ref, msg) =>
      protocol.Question(
        id,
        senderOpt.map(x => actorRefToString(x)),
        ref,
        MessageSerialization.render(msg)
      )

    case Answer(questionId, msg) =>
      protocol.Answer(questionId, MessageSerialization.render(msg))

    case AnswerFailed(questionId, ex) =>
      protocol.AnswerFailed(questionId, MessageSerialization.render(ex))

    case ReportingDisabled =>
      protocol.ReportingDisabled
    case ReportingEnabled =>
      protocol.ReportingEnabled
    case SnapshotAvailable(s) =>
      protocol.SnapshotAvailable(
        s.liveActors.map(ref => ref -> s.classNameFor(ref))(breakOut),
        s.dead.map(ref => ref -> s.classNameFor(ref))(breakOut),
        s.receivedFrom
      )
    case ThroughputMeasurement(ref, msgs, ts) =>
      protocol.ThroughputMeasurement(ref, msgs, ts)
  }

}

object BackendEventsMarshalling extends BackendEventsMarshalling
