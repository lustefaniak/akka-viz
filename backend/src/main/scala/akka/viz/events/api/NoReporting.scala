package akka.viz.events.api

import akka.actor.{Actor, ActorRef, InternalActorRef}

object NoReporting extends ReportingApi {
  override def received(sender: ActorRef, receiver: ActorRef, message: Any): Unit = {}

  override def mailboxStatus(ref: ActorRef, numberOfMessages: Int): Unit = {}

  override def killed(self: ActorRef): Unit = {}

  override def currentActorState(self: InternalActorRef, actor: Actor): Unit = {}

  override def instantiated(self: ActorRef, actor: Actor): Unit = {}

  override def spawned(self: ActorRef, parent: ActorRef): Unit = {}

  override def fsmTransition(self: ActorRef, currentState: Any, stateData: Any, nextState: Any, nextStateData: Any): Unit = {}
}
