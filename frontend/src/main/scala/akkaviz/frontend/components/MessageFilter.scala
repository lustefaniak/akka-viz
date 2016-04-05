package akkaviz.frontend.components

import org.scalajs.dom.html._
import org.scalajs.dom.{Element => domElement, console}
import rx.{Rx, Var}

import scala.scalajs.js.ThisFunction0
import scala.util.Try
import scalatags.JsDom.all._

class MessageFilter(
    seenMessages: Var[Set[String]],
    selectedMessages: Var[Set[String]],
    selectedActors: Var[Set[String]]
) extends Tab {
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

  val elem = div(cls := "panel-body", id := "messagefilter",
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

  override def name: String = "Message filter"

  override def tabId: String = "message-filter"

  override def onCreate(): Unit = {
    tabBody.appendChild(elem)
  }
}
