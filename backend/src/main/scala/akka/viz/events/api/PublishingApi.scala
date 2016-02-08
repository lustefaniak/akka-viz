package akka.viz.events.api

import akka.actor.{InternalActorRef, Actor, ActorRef}
import akka.viz.events.types._

class PublishingApi(publish: (InternalEvent) => Unit) extends ReportingApi {
  override def mailboxStatus(ref: ActorRef, numberOfMessages: Int): Unit = {
    publish(MailboxStatus(ref, numberOfMessages))
  }

  override def received(sender: ActorRef, receiver: ActorRef, message: Any): Unit = {
    publish(Received(sender, receiver, message))
  }

  override def killed(self: ActorRef): Unit = {
    publish(Killed(self))
  }

  override def currentActorState(self: InternalActorRef, actor: Actor): Unit = {
    publish(CurrentActorState(self, actor))
  }

  override def instantiated(self: ActorRef, actor: Actor): Unit = {
    publish(Instantiated(self, actor))
  }

  override def spawned(self: ActorRef, parent: ActorRef): Unit = {
    publish(Spawned(self, parent))
  }

  override def fsmTransition(self: ActorRef, currentState: Any, stateData: Any, nextState: Any, nextStateData: Any): Unit = {
    publish(FSMTransition(self, currentState, stateData, nextState, nextStateData))
  }
}

case object PublishingApi {
  def apply(publish: (InternalEvent) => Unit): PublishingApi = {
    new PublishingApi(publish)
  }
}