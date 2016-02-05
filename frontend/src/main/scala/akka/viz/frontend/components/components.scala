package akka.viz.frontend.components

import akka.viz.frontend.FrontendUtil._
import akka.viz.frontend.{PrettyJson, DOMGlobalScope}
import org.scalajs.dom.raw.Event

import scala.collection.mutable

import akka.viz.protocol.{Received, SetAllowedMessages}
import org.scalajs.dom.{Element => domElement, Node, console}
import org.scalajs.dom.html._
import rx.{Rx, Var}

import scala.scalajs.js
import scala.scalajs.js.{ThisFunction, ThisFunction0}
import scala.util.Try
import scalatags.JsDom.all._

trait Component {
  def render: Element
}

class ActorSelector(seenActors: Var[Set[String]], selectedActors: Var[Set[String]],
    currentActorState: mutable.Map[String, String], // FIXME use var here?
    actorClasses: String => Var[js.UndefOr[String]]) extends PrettyJson with Component {

  val popoverContent: ThisFunction0[domElement, Node] = (that: domElement) => {
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
          td(actorName, if (isSelected) fontWeight.bold else ()), onclick := {
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

  def render = {
    div(cls := "panel-body", id := "actortree",
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
  }
}

class MessageFilter(
  seenMessages: Var[Set[String]],
    selectedMessages: Var[Set[String]],
    selectedActors: Var[Set[String]]
) extends Component {
  val messagesObs = Rx.unsafe {
    (seenMessages(), selectedMessages())
  }.triggerLater {

    val seen = seenMessages.now.toList.sorted
    val selected = selectedMessages.now

    val content = seen.map {
      clazz =>
        val contains = selected(clazz)
        tr(
          td(input(`type` := "checkbox", if (contains) checked else ())),
          td(if (contains) b(clazz) else clazz),
          onclick := {
            () =>
              console.log(s"Toggling ${clazz} now it will be ${!contains}")
              selectedMessages() = if (contains) selected - clazz else selected + clazz
          }
        )
    }

    messagesTbody.innerHTML = ""
    messagesTbody.appendChild(content.render)
  }

  lazy val messagesTbody = tbody().render

  def render = {
    div(cls := "panel-body", id := "messagefilter",
      table(
        cls := "table table-striped table-hover",
        thead(
          tr(th(), th("Class", p(
            float.right,
            input(id := "messagefilter-regex", size := 12, tpe := "text", placeholder := "Filter...", marginRight := 1.em, onkeyup := regexMessageFilter),
            a(href := "#", id := "messagefilter-select-all", "all", onclick := selectAllMessageFilters),
            " | ",
            a(href := "#", id := "messagefilter-select-none", "none", onclick := clearMessageFilters)
          )))
        ),
        messagesTbody
      )).render
  }

  def clearMessageFilters: ThisFunction0[domElement, Unit] = { _: domElement =>
    selectedMessages() = Set.empty
  }

  def selectAllMessageFilters: ThisFunction0[domElement, Unit] = { _: domElement =>
    selectedMessages() = seenMessages.now
  }

  def regexMessageFilter(): ThisFunction0[Input, Unit] = { self: Input =>
    val input = self.value
    Try(input.r).foreach { r =>
      selectedMessages() = seenMessages.now.filter(_.matches(r.regex))
    }
  }

}

class MessagesPanel(selectedActors: Var[Set[String]]) extends Component with PrettyJson {

  def messageReceived(rcv: Received): Unit = {
    def insert(e: Element): Unit = {
      messagesTbody.appendChild(e)
    }
    val uid = rcv.eventId
    val sender = actorName(rcv.sender)
    val receiver = actorName(rcv.receiver)
    val selected = selectedActors.now

    if (selected.contains(sender) || selected.contains(receiver)) {

      val mainRow = tr(
        "data-toggle".attr := "collapse",
        "data-target".attr := s"#detail$uid",
        td(sender),
        td(receiver),
        td(rcv.payloadClass)
      )

      val payload: String = rcv.payload.getOrElse("")
      val detailsRow = tr(
        id := s"detail$uid",
        cls := "collapse",
        td(
          colspan := 3,
          div(pre(prettyPrintJson(payload))) // FIXME: display formated lazily
        )
      )

      insert(mainRow.render)
      insert(detailsRow.render)
    }
  }

  selectedActors.trigger {
    if (selectedActors.now.isEmpty) {
      messagePanelTitle.innerHTML = s"Select actor to show its messages"
    } else {
      messagePanelTitle.innerHTML = s"Messages"
    }
    messagesTbody.innerHTML = ""
  }

  lazy val messagePanelTitle = div(cls := "panel-heading", id := "messagespaneltitle", "Messages").render
  lazy val messagesTbody = tbody().render

  override def render: Element = {
    div(
      cls := "panel panel-default",
      messagePanelTitle,
      div(cls := "panel-body", id := "messagespanelbody", overflowY.scroll, overflowX.scroll, maxHeight := 400.px,
        table(
          cls := "table table-striped table-hover",
          thead(
            tr(th("From"), th("To"), th("Class"))
          ), messagesTbody
        ))
    ).render
  }
}

