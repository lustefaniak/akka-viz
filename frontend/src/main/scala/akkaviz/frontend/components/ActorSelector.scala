package akkaviz.frontend.components

import akkaviz.frontend.{ActorPath, FrontendUtil, PrettyJson}
import akkaviz.protocol.ActorFailure
import org.scalajs.dom.html._
import org.scalajs.dom.{Element => domElement, console, document}
import rx.{Ctx, Rx, Var}

import scala.scalajs.js.ThisFunction0
import scala.util.Try
import scalatags.JsDom.all._

class ActorSelector(
    seenActors: Var[Set[ActorPath]],
    selectedActors: Var[Set[ActorPath]],
    actorFailures: Var[Seq[ActorFailure]],
    detailsOpener: (ActorPath) => Unit
) extends PrettyJson with Tab {

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

  private[this] def exceptionsButton(actorRef: ActorPath, failures: Seq[ActorFailure]) =
    span(
      style := "color: red",
      `class` := "imgbtn glyphicon glyphicon-exclamation-sign",
      onclick := { () =>
        detailsOpener(actorRef)
      }
    )

  private[this] def detailsButton(actorRef: ActorPath) =
    span(
      `class` := "glyphicon glyphicon-info-sign",
      onclick := { () =>
        detailsOpener(actorRef)
      }
    )

  private[this] def actorExceptionsIndicator(actorRef: ActorPath, failures: Seq[ActorFailure]): _root_.scalatags.JsDom.Modifier =
    if (failures.isEmpty) ""
    else span(b(s"${failures.length} "), exceptionsButton(actorRef, failures))

  private[this] val actorsObs = Rx.unsafe {
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
            span(FrontendUtil.shortActorName(actorRef)),
            span(float.right, actorExceptionsIndicator(actorRef, actorFailures.now.filter(_.actorRef == actorRef)), detailsButton(actorRef))
          )
        )(data("actor") := actorRef).render

        element
    }

    actorTreeTbody.innerHTML = ""
    actorTreeTbody.appendChild(content.render)
  }

  def toggleActor(actorRef: ActorPath): Unit = {
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

  override def name: String = "Actor filter"

  override def tabId: String = "actor-filter"

  override def onCreate(): Unit = {
    tabBody.appendChild(elem)
  }
}
