package akka.viz.events.api

import akka.actor.{Actor, ActorRef, InternalActorRef}

trait ReportingApi {
  def killed(self: ActorRef): Unit

  def fsmTransition(self: ActorRef, currentState: Any, stateData: Any, nextState: Any, nextStateData: Any): Unit

  def instantiated(self: ActorRef, actor: Actor): Unit

  def spawned(self: ActorRef, parent: ActorRef): Unit

  def currentActorState(self: InternalActorRef, actor: Actor): Unit

  def received(sender: ActorRef, receiver: ActorRef, message: Any): Unit

  def mailboxStatus(ref: ActorRef, numberOfMessages: Int): Unit

}
