package akkaviz.frontend.components

import akkaviz.frontend.ActorRepository.ActorState
import akkaviz.frontend.DOMGlobalScope.$
import akkaviz.frontend.FrontendUtil.actorComponent
import akkaviz.frontend.{DOMGlobalScope, PrettyJson}
import akkaviz.protocol.ActorFailure
import org.scalajs.dom.html._
import org.scalajs.dom.{Element => domElement, Node, console}
import rx.{Rx, Var}

import scala.scalajs.js
import scala.scalajs.js.ThisFunction0
import scala.util.Try
import scalatags.JsDom.all._

class ActorSelector(
    seenActors: Var[Set[String]],
    selectedActors: Var[Set[String]],
    currentActorState: (String) => Var[ActorState],
    actorFailures: Var[Seq[ActorFailure]]
) extends PrettyJson with Component {

  val popoverContent: ThisFunction0[domElement, Node] = (that: domElement) => {
    val actor: String = that.getAttribute("data-actor")
    val content = div().render
    val stateVar = currentActorState(actor)
    stateVar.trigger {
      val state = stateVar.now
      val renderedState = Seq[Frag](
        div(strong("Class: "), state.className.getOrElse[String]("Unknown class")),
        div(strong("Is dead: "), state.isDead.toString),
        div(strong("Internal state: "), pre(state.internalState.map(prettyPrintJson).getOrElse[String]("Internal state unknown"))),
        div(strong("Is FSM: "), state.fsmState.isDefined.toString),
        state.fsmState.map[Frag] {
          fsm =>
            Seq(
              div(strong("FSM State: "), pre(prettyPrintJson(fsm.currentState))),
              div(strong("FSM Data: "), pre(prettyPrintJson(fsm.currentData)))
            )
        }.getOrElse(()),
        div(strong("Mailbox size: "), state.mailboxSize.map(_.toString).getOrElse[String]("Unknown")),
        div(strong("Last updated: "), state.lastUpdatedAt.toISOString())
      ).render

      content.innerHTML = ""
      content.appendChild(renderedState)
    }

    Seq[Frag](
      h5(actor),
      content
    ).render
  }

  val popoverOptions = js.Dictionary(
    "content" -> popoverContent,
    "trigger" -> "hover",
    "placement" -> "right",
    "html" -> true
  )

  def failureTable(failures: Seq[ActorFailure]) =
    table(
      id := "failures-table",
      `class` := "table",
      thead(
        tr(th("Exception", cls := "col-md-6"), th("Supervisor decision", cls := "col-md-1"), th("Time", cls := "col-md-5"))
      ),
      tbody(
        for (f <- failures)
          yield tr(td(f.cause), td(f.decision), td(f.ts))
      )
    ).render

  def exceptionsButton(actorName: String, failures: Seq[ActorFailure]) =
    span(
      style := "color: red",
      `class` := "glyphicon glyphicon-exclamation-sign",
      "data-toggle".attr := "modal",
      "data-target".attr := "#failures-modal",
      onclick := { () =>
        $("#actor-name").html(actorName)
        $("#actor-failures").html(failureTable(failures))
      }
    )

  def actorExceptionsIndicator(actorName: String, failures: Seq[ActorFailure]): _root_.scalatags.JsDom.Modifier =
    if (failures.isEmpty) ""
    else span(b(s"${failures.length} "), exceptionsButton(actorName, failures))

  val actorsObs = Rx.unsafe {
    (seenActors(), selectedActors(), actorFailures())
  }.trigger {
    val seen = seenActors.now.toList.sorted
    val selected = selectedActors.now

    val content = seen.map {
      actorName =>
        val isSelected = selected.contains(actorName)
        val element = tr(
          td(input(`type` := "checkbox", if (isSelected) checked else (),
            onclick := {
              () => toggleActor(actorName)
            })),
          td(
            actorComponent(actorName),
            span(float.right, actorExceptionsIndicator(actorName, actorFailures.now.filter(_.actorRef == actorName)))
          )
        )(data("actor") := actorName).render

        DOMGlobalScope.$(element).popover(popoverOptions)
        element
    }

    actorTreeTbody.innerHTML = ""
    actorTreeTbody.appendChild(content.render)
  }

  def toggleActor(actorPath: String): Unit = {
    if (selectedActors.now contains actorPath) {
      console.log(s"Unselected '$actorPath' actor")
      selectedActors() = selectedActors.now - actorPath
    } else {
      console.log(s"Selected '$actorPath' actor")
      selectedActors() = selectedActors.now + actorPath
    }
  }

  lazy val actorTreeTbody = tbody().render

  def clearActorFilters: ThisFunction0[domElement, Unit] = { self: domElement =>
    selectedActors() = Set.empty
  }

  def selectAllActorFilters: ThisFunction0[domElement, Unit] = { self: domElement =>
    selectedActors() = seenActors.now
  }

  def regexActorFilter: ThisFunction0[domElement, Unit] = { self: domElement =>
    val input = self.asInstanceOf[Input].value
    Try(input.r).foreach { r =>
      selectedActors() = seenActors.now.filter(_.matches(r.regex))
    }
  }

  def attach(parent: domElement): Unit = {
    val elem = div(cls := "panel-body", id := "actortree",
      table(
        cls := "table table-striped table-hover",
        thead(
          tr(th(), th("Actor", p(
            float.right,
            input(id := "actorfilter-regex", size := 12, tpe := "text", placeholder := "Filter...", marginRight := 1.em, onkeyup := regexActorFilter),
            a(href := "#", id := "actorfilter-select-all", "all", onclick := selectAllActorFilters),
            " | ",
            a(href := "#", id := "actorfilter-select-none", "none", onclick := clearActorFilters)
          )))
        ),
        actorTreeTbody
      )).render
    parent.appendChild(elem)
  }
}
