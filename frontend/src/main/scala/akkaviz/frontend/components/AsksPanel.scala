package akkaviz.frontend.components

import akkaviz.frontend.FrontendUtil.shortActorName
import akkaviz.frontend.PrettyJson
import akkaviz.protocol.{Answer, AnswerFailed, AskResult, Question}
import org.scalajs.dom.{Element, document}
import rx.Var

import scala.collection.mutable
import scalatags.JsDom.all._

class AsksPanel(selectedActors: Var[Set[String]]) extends Tab with PrettyJson {

  private[this] val asks: mutable.Set[Question] = mutable.Set()

  private[this] lazy val panelBody = div(
    cls := "panel-body",
    id := "askspanelbody",
    panelTable
  )

  private[this] lazy val panelTable = table(
    cls := "table table-striped table-hover",
    tableHead,
    tableBody
  )

  private[this] lazy val tableHead = thead(
    tr(th("From"), th("To"), th("Status"))
  )

  private[this] lazy val tableBody = tbody().render

  def receivedQuestion(q: Question): Unit = {
    asks += q
    tableBody.appendChild(tableRowAwaiting(q).render)
  }

  def receivedAnswer(a: Answer): Unit = {
    val question = asks.find(_.id == a.questionId)
    if (question.isDefined) {
      val oldRow = tableBody.querySelector(s"#question-${a.questionId}")
      tableBody.replaceChild(tableRowAnswered(question.get, a).render, oldRow)
    }
  }

  def receivedAnswerFailed(af: AnswerFailed): Unit = {
    val question = asks.find(_.id == af.questionId)
    if (question.isDefined) {
      val oldRow = tableBody.querySelector(s"#question-${af.questionId}")
      tableBody.replaceChild(tableRowAnswerFailed(question.get, af).render, oldRow)
    }
  }

  private[this] def tableRowAwaiting(question: Question) =
    tableRow(question, None)

  private[this] def tableRowAnswered(question: Question, answer: Answer) =
    tableRow(question, Some(answer))

  private[this] def tableRowAnswerFailed(question: Question, ansFailed: AnswerFailed) =
    tableRow(question, Some(ansFailed)).apply(cls := "danger")

  private[this] def tableRow(question: Question, result: Option[AskResult]) = {
    val details = detailsView(question, result)
    val status = statusText(result)
    tr(
      "data-toggle".attr := "modal",
      "data-target".attr := "#ask-modal",
      id := s"question-${question.id}",
      td(question.sender.map(shortActorName).getOrElse("No sender"): String),
      td(shortActorName(question.actorRef)),
      td(status),
      onclick := { () =>
        val elem = document.getElementById("ask-modal-body")
        elem.innerHTML = ""
        elem.appendChild(details.render)
      }
    )
  }

  private[this] def detailsView(question: Question, result: Option[AskResult]) = result match {
    case None                          => unansweredQuestionDetails(question)
    case Some(ans: Answer)             => answeredQuestionDetails(question, ans)
    case Some(ansFailed: AnswerFailed) => failedAnswerDetails(question, ansFailed)
  }

  private[this] def statusText(result: Option[AskResult]) = result match {
    case None                          => "Waiting for answer"
    case Some(ans: Answer)             => "Answered"
    case Some(ansFailed: AnswerFailed) => "Failed to answer"
  }

  private[this] def unansweredQuestionDetails(q: Question) =
    div(
      questionHeader(q),
      pre(prettyPrintJson(q.message)),
      h4(s"Waiting for answer from ${q.actorRef}")
    )

  private[this] def answeredQuestionDetails(q: Question, a: Answer) =
    div(
      questionHeader(q),
      pre(prettyPrintJson(q.message)),
      h3("Answer from " + q.actorRef),
      pre(prettyPrintJson(a.message))
    )

  private[this] def failedAnswerDetails(q: Question, af: AnswerFailed) =
    div(
      questionHeader(q),
      pre(prettyPrintJson(q.message)),
      h3("Exception"),
      pre(af.ex)
    )

  private[this] def questionHeader(q: Question) = q.sender match {
    case Some(s) => h3(s"Question from $s")
    case None    => h3("Question(no sender)")
  }

  override def name: String = "Asks"

  override def tabId: String = "ask-list"

  override def onCreate(): Unit = tabBody.appendChild(panelBody.render)
}
