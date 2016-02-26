package akkaviz.frontend.components

import akkaviz.protocol.{AnswerFailed, Answer, Question}
import rx.Var

import scala.collection.mutable
import scalatags.JsDom.all._


class AsksPanel(selectedActors: Var[Set[String]]) extends Component {

  private val asks: mutable.Map[Question, Option[Either[Answer, AnswerFailed]]] = mutable.Map()


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
    asks += (q -> None)
    tableBody.appendChild(tableRowAwaiting(q).render)
  }

  def receivedAnswer(a: Answer): Unit = {
    val question = asks.keysIterator.find(_.id == a.questionId)
    if (question.isDefined) {
      val oldRow = tableBody.querySelector(s"#question-${a.questionId}")
      tableBody.replaceChild(tableRowAnswered(question.get, a).render, oldRow)
    }
  }

  def receivedAnswerFailed(a: AnswerFailed): Unit = {
    val question = asks.keysIterator.find(_.id == a.questionId)
    if (question.isDefined) {
      val oldRow = tableBody.querySelector(s"#question-${a.questionId}")
      tableBody.replaceChild(tableRowAnswerFailed(question.get, a).render, oldRow)
    }
  }

  private def tableRowAwaiting(question: Question) =
    tr(
      id := s"question-${question.id}",
      td(question.sender.getOrElse("No sender"): String),
      td(question.actorRef),
      td("Awaiting answer")
    )

  private def tableRowAnswered(question: Question, answer: Answer) =
    tr(
      id := s"question-${question.id}",
      td(question.sender.getOrElse("No sender"): String),
      td(question.actorRef),
      td("Answered")
    )

  private def tableRowAnswerFailed(question: Question, ansFailed: AnswerFailed) =
    tr(
      id := s"question-${question.id}",
      td(question.sender.getOrElse("No sender"): String),
      td(question.actorRef),
      td("Answer failed")
    )

}
