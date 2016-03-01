package akkaviz.frontend.components

import akkaviz.frontend.DOMGlobalScope._
import akkaviz.frontend.FrontendUtil.shortActorName
import akkaviz.frontend.PrettyJson
import akkaviz.protocol.{Answer, AnswerFailed, AskResult, Question}
import rx.Var

import scala.collection.mutable
import scalatags.JsDom.all._

class AsksPanel(selectedActors: Var[Set[String]]) extends Component with PrettyJson {

  private val asks: mutable.Set[Question] = mutable.Set()

  override def render = panelBody.render

  lazy val panelBody = div(
    cls := "panel-body",
    id := "askspanelbody",
    panelTable
  )

  lazy val panelTable = table(
    cls := "table table-striped table-hover",
    tableHead,
    tableBody
  )

  lazy val tableHead = thead(
    tr(th("From"), th("To"), th("Status"))
  )

  lazy val tableBody = tbody().render

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

  private def tableRowAwaiting(question: Question) =
    tableRow(question, None)

  private def tableRowAnswered(question: Question, answer: Answer) =
    tableRow(question, Some(answer))

  private def tableRowAnswerFailed(question: Question, ansFailed: AnswerFailed) =
    tableRow(question, Some(ansFailed)).apply(cls := "danger")

  private def tableRow(question: Question, result: Option[AskResult]) = {
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
        $("#ask-modal-body").html(details.render)
      }
    )
  }

  def detailsView(question: Question, result: Option[AskResult]) = result match {
    case None => unansweredQuestionDetails(question)
    case Some(ans: Answer) => answeredQuestionDetails(question, ans)
    case Some(ansFailed: AnswerFailed) => failedAnswerDetails(question, ansFailed)
  }

  def statusText(result: Option[AskResult]) = result match {
    case None => "Waiting for answer"
    case Some(ans: Answer) => "Answered"
    case Some(ansFailed: AnswerFailed) => "Failed to answer"
  }

  def unansweredQuestionDetails(q: Question) =
    div(
      questionHeader(q),
      pre(prettyPrintJson(q.message)),
      h4(s"Waiting for answer from ${q.actorRef}")
    )

  def answeredQuestionDetails(q: Question, a: Answer) =
    div(
      questionHeader(q),
      pre(prettyPrintJson(q.message)),
      h3("Answer from " + q.actorRef),
      pre(prettyPrintJson(a.message))
    )

  def failedAnswerDetails(q: Question, af: AnswerFailed) =
    div(
      questionHeader(q),
      pre(prettyPrintJson(q.message)),
      h3("Exception"),
      pre(af.ex)
    )

  def questionHeader(q: Question) = q.sender match {
    case Some(s) => h3(s"Question from $s")
    case None => h3("Question(no sender)")
  }

}
