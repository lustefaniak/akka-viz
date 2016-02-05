package akka.viz.frontend.components

import akka.viz.frontend.DOMGlobalScope
import akka.viz.frontend.FrontendApp._
import org.scalajs.dom.{onclick => _, _}
import rx.{Rx, Var}

import scala.scalajs.js
import scala.scalajs.js.ThisFunction0
import scalatags.JsDom.all._

class ActorSelector(seenActors: Var[Set[String]], selectedActors: Var[Set[String]]) {


  val popoverContent: ThisFunction0[Element, Node] = (that: Element) => {
    val actor: String = that.getAttribute("data-actor")
    val actorState: String = currentActorState.get(actor).map(prettyPrintJson).getOrElse("Internal state unknown")
    val popover = Seq(
      h5(actor),
      h6("Class: " + actorClasses(actor).now.getOrElse("")),
      pre(actorState)
    )

    val elem = popover.render
    elem
  }

  val popoverOptions = js.Dictionary(
    "content" -> popoverContent,
    "trigger" -> "hover",
    "placement" -> "right",
    "html" -> true
  )

  val actorsObs = Rx.unsafe {
    (seenActors(), selectedActors())
  }.trigger {
    val seen = seenActors.now.toList.sorted
    val selected = selectedActors.now

    val content = seen.map {
      actorName =>
        val isSelected = selected.contains(actorName)
        val element = tr(
          td(input(`type` := "checkbox", if (isSelected) checked else ())),
          td(if (isSelected) b(actorName) else actorName), onclick := {
            () => toggleActor(actorName)
          }
        )(data("actor") := actorName).render

        DOMGlobalScope.$(element).popover(popoverOptions)
        element
    }

//    val actorTree = document.querySelector("#actortree tbody")
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



  def render: Element = {
    div(`class` := "panel-body", id:="actortree",
      table(`class`:= "table table-striped table-hover",
        thead(
          tr(th(), th("Actor", p(float.right,
            input(id := "actorfilter-regex", size := 12, tpe := "text", placeholder := "Filter..."),
            a(href:="#", id:="actorfilter-select-all", "all"), " | ", a(href:="#", id:="actorfilter-select-none", "none")
          )))
        ),
        actorTreeTbody
      )
    ).render
  }
}