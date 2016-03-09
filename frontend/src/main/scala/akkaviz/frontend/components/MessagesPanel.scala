package akkaviz.frontend.components

import akkaviz.frontend.FrontendUtil.shortActorName
import akkaviz.frontend.{FrontendUtil, PrettyJson}
import akkaviz.protocol.Received
import org.scalajs.dom.html._
import org.scalajs.dom.raw.MouseEvent
import org.scalajs.dom.{Element => domElement, console}
import rx.Var

import scala.collection.immutable.Queue
import scala.scalajs.js.ThisFunction1
import scalatags.JsDom.all._

class MessagesPanel(selectedActors: Var[Set[String]]) extends Component with PrettyJson {
  private[this] val ShowMoreLength = 200

  private[this] val msgQueue = Var[Queue[Received]](Queue.empty)

  private[this] var lastDisplayed = 0L

  def messageReceived(rcv: Received): Unit = {
    val selected = selectedActors.now
    if (selected.contains(rcv.sender) || selected.contains(rcv.receiver)) {
      if (messagesTbody.childNodes.length < 50) {
        messagesTbody.appendChild(messageRow(rcv).render)
        lastDisplayed = rcv.eventId
      } else {
        msgQueue() = msgQueue.now.enqueue(rcv)
      }
    }
  }

  private[this] val showMoreRow = tr(cell, onclick := displayMoreMessages).render
  private[this] lazy val cell = td(colspan := 3, fontStyle.italic).render

  private[this] def displayMoreMessages: ThisFunction1[TableRow, MouseEvent, Unit] = { (row: TableRow, ev: MouseEvent) =>
    ev.preventDefault()
    val (portion, q) = (msgQueue.now.take(ShowMoreLength), msgQueue.now.drop(ShowMoreLength))
    msgQueue() = q
    portion.foreach { rcv =>
      messagesTbody.appendChild(messageRow(rcv).render)
      lastDisplayed = rcv.eventId
    }
  }

  private[this] def toggleVisibility(e: domElement): Unit = {
    val elem = e.asInstanceOf[Element]
    if (elem.style.display == "none")
      elem.style.display = ""
    else elem.style.display = "none"
  }

  private[this] val toggleMessageDetails = (mouseEvent: MouseEvent) => {
    mouseEvent.preventDefault()
    console.log(mouseEvent.srcElement)
    FrontendUtil.findParentWithAttribute(mouseEvent.srcElement, "data-message").foreach {
      row =>
        val nextRow = row.nextElementSibling
        if (nextRow == null || nextRow.hasAttribute("data-message")) {
          val payload = row.getAttribute("data-message")
          val detailsRow = tr(
            td(
              colspan := 3,
              div(pre(prettyPrintJson(payload)))
            )
          ).render
          row.parentNode.insertBefore(detailsRow, nextRow)
        } else {
          toggleVisibility(nextRow)
        }
    }
  }

  private[this] def messageRow(rcv: Received) = {
    tr(
      "data-message".attr := rcv.payload.getOrElse(""),
      `class` := "tgl",
      td(shortActorName(rcv.sender)),
      td(shortActorName(rcv.receiver)),
      td(rcv.payloadClass, if (!rcv.handled) unhandledIndicator else ""),
      onclick := toggleMessageDetails
    )
  }

  private[this] val unhandledIndicator = span(
    style := "color: orange; vertical-align: middle; float: right",
    `class` := "glyphicon glyphicon-exclamation-sign",
    title := "Unhandled message"
  )

  private[this] lazy val messagePanelTitle = span("Messages").render
  private[this] lazy val messagePanelHeader = div(
    cls := "panel-heading", id := "messagespaneltitle",
    messagePanelTitle,
    a(href := "#", float.right, onclick := clearMessages, i(`class` := "material-icons", "delete"))
  ).render
  private[this] lazy val messagesTbody = tbody().render

  private[this] val clearMessages = () => {
    messagesTbody.innerHTML = ""
    msgQueue() = Queue.empty
  }

  override def attach(parent: domElement): Unit = {
    val elem = div(
      cls := "panel panel-default",
      messagePanelHeader,
      div(cls := "panel-body", id := "messagespanelbody",
        table(
          cls := "table table-striped table-hover",
          thead(
            tr(th("From"), th("To"), th("Class"))
          ), messagesTbody, tfoot(showMoreRow)
        ))
    ).render

    msgQueue.foreach { q =>
      if (q.headOption.exists(_.eventId > lastDisplayed))
        cell.innerHTML = s"${q.length} messages not shown, click to display more"
      else cell.innerHTML = ""
    }

    selectedActors.trigger {
      if (selectedActors.now.isEmpty) {
        messagePanelTitle.innerHTML = s"Select actor to show its messages"
      } else {
        messagePanelTitle.innerHTML = s"Messages"
      }
    }

    parent.appendChild(elem)
  }
}
