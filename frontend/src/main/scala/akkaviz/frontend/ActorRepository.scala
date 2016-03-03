package akkaviz.frontend

import rx.Var

import scala.scalajs.js
import scala.scalajs.js.{UndefOr, Date}

class ActorRepository {

  import ActorRepository._

  private val currentActorState = js.Dictionary[Var[ActorState]]()

  val seenActors = Var[Set[String]](Set())

  def state(actor: String): Var[ActorState] = currentActorState.getOrElseUpdate(actor, Var(ActorState(actor, FrontendUtil.shortActorName(actor), FrontendUtil.systemName(actor))))

  def mutateActor(actor: String)(fn: ActorState => ActorState) = {
    state(actor)() = fn(state(actor).now.copy(lastUpdatedAt = new js.Date()))
    addActorsToSeen(actor)
  }

  @inline
  final def addActorsToSeen(actors: String*): Unit = {
    addActorsToSeen(actors)
  }

  def addActorsToSeen(actors: Iterable[String]): Unit = {
    val previouslySeen = seenActors.now
    val newSeen = previouslySeen ++ actors
    if (previouslySeen.size != newSeen.size)
      seenActors() = newSeen
  }

}

case object ActorRepository {

  case class FSMState(currentState: String, currentData: String)

  case class ActorState(
    path: String,
    label: String,
    system: String,
    isDead: Boolean = false,
    mailboxSize: js.UndefOr[Int] = js.undefined,
    internalState: js.UndefOr[String] = js.undefined,
    className: js.UndefOr[String] = js.undefined,
    fsmState: js.UndefOr[FSMState] = js.undefined,
    fsmTransitions: Set[(String, String)] = Set(),
    lastUpdatedAt: js.Date = new Date()
  )

}