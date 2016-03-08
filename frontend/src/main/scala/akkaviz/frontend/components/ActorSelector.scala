package akkaviz.frontend.components

import akkaviz.frontend.ActorRepository.ActorState
import akkaviz.frontend.FrontendUtil.actorComponent
import akkaviz.frontend.{DOMGlobalScope, PrettyJson}
import akkaviz.protocol
import akkaviz.protocol.ActorFailure
import org.scalajs.dom.html._
import org.scalajs.dom.{Element => domElement, Node, console, document}
import rx.{Rx, Var}

import scala.scalajs.js
import scala.scalajs.js.ThisFunction0
import scala.util.Try
import scalatags.JsDom.all._

class ActorSelector(
    seenActors: Var[Set[String]],
    selectedActors: Var[Set[String]],
    currentActorState: (String) => Var[ActorState],
    actorFailures: Var[Seq[ActorFailure]],
    upstreamSend: protocol.ApiClientMessage => Unit
) extends PrettyJson with Component {

  var fsmGraph: js.UndefOr[FsmGraph] = js.undefined

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

    fsmGraph = new FsmGraph(document.getElementById("actor-fsm").asInstanceOf[Element])

    parent.appendChild(elem)
  }

  private[this] val popoverContent: ThisFunction0[domElement, Node] = (that: domElement) => {
    val actor: String = that.getAttribute("data-actor")
    val content = div().render
    val stateVar = currentActorState(actor)
    //renderActorState(content, stateVar) //fixme!
    Seq[Frag](
      h5(actor),
      content
    ).render
  }

  private[this] val popoverOptions = js.Dictionary(
    "content" -> popoverContent,
    "trigger" -> "hover",
    "placement" -> "right",
    "html" -> true
  )

  private[this] def failureTable(failures: Seq[ActorFailure]) =
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

  private[this] def exceptionsButton(actorRef: String, failures: Seq[ActorFailure]) =
    span(
      style := "color: red",
      `class` := "imgbtn glyphicon glyphicon-exclamation-sign",
      "data-toggle".attr := "modal",
      "data-target".attr := "#failures-modal",
      onclick := { () =>
        document.getElementById("actor-name").innerHTML = actorRef
        document.getElementById("actor-failures").innerHTML = ""
        document.getElementById("actor-failures").appendChild(failureTable(failures))
      }
    )

  private[this] def detailsButton(actorRef: String) =
    span(
      `class` := "glyphicon glyphicon-info-sign",
      onclick := { () =>
        val stateVar = currentActorState(actorName)

        new ActorStateTab(stateVar).attach(document.querySelector("#right-pane"))

        fsmGraph.foreach {
          fsmGraph =>
            fsmGraph.displayFsm(stateVar.now.fsmTransitions)
        }
      }
    )

  private[this] def refreshButton(actorRef: String) =
    span(
      `class` := "imgbtn glyphicon glyphicon-refresh",
      onclick := { () =>
        upstreamSend(protocol.RefreshInternalState(actorRef))
      }
    )

  private[this] def actorExceptionsIndicator(actorRef: String, failures: Seq[ActorFailure]): _root_.scalatags.JsDom.Modifier =
    if (failures.isEmpty) ""
    else span(b(s"${failures.length} "), exceptionsButton(actorRef, failures))

  val actorsObs = Rx.unsafe {
    (seenActors(), selectedActors(), actorFailures())
  }.trigger {
    val seen = seenActors.now.toList.sorted
    val selected = selectedActors.now

    val content = seen.map {
      actorRef =>
        val isSelected = selected.contains(actorRef)
        val element = tr(
          td(input(`type` := "checkbox", if (isSelected) checked else (),
            onclick := {
              () => toggleActor(actorRef)
            })),
          td(
            actorComponent(actorRef),
            span(float.right, actorExceptionsIndicator(actorRef, actorFailures.now.filter(_.actorRef == actorRef)), refreshButton(actorRef), detailsButton(actorRef))
          )
        )(data("actor") := actorRef).render

        DOMGlobalScope.$(element).popover(popoverOptions)
        element
    }

    actorTreeTbody.innerHTML = ""
    actorTreeTbody.appendChild(content.render)
  }

  def toggleActor(actorRef: String): Unit = {
    if (selectedActors.now contains actorRef) {
      console.log(s"Unselected '$actorRef' actor")
      selectedActors() = selectedActors.now - actorRef
    } else {
      console.log(s"Selected '$actorRef' actor")
      selectedActors() = selectedActors.now + actorRef
    }
  }

  private[this] lazy val actorTreeTbody = tbody().render

  private[this] def clearActorFilters: ThisFunction0[domElement, Unit] = { self: domElement =>
    selectedActors() = Set.empty
  }

  private[this] def selectAllActorFilters: ThisFunction0[domElement, Unit] = { self: domElement =>
    selectedActors() = seenActors.now
  }

  private[this] def regexActorFilter: ThisFunction0[domElement, Unit] = { self: domElement =>
    val input = self.asInstanceOf[Input].value
    Try(input.r).foreach { r =>
      selectedActors() = seenActors.now.filter(_.matches(r.regex))
    }
  }

}
