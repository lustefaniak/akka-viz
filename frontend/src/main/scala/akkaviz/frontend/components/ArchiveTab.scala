package akkaviz.frontend.components

import akkaviz.frontend.FrontendUtil
import akkaviz.rest
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.{Element => domElement}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Date
import scalatags.JsDom.all._

trait ArchiveTab extends ClosableTab {

  import akkaviz.frontend.PrettyJson._

  def loadUrl: String

  private[this] var loading: js.UndefOr[Future[dom.XMLHttpRequest]] = js.undefined

  private[this] def loadBetween(): Unit = {
    if (loading.isEmpty) {
      messagesTbody.innerHTML = ""
      messagesTbody.appendChild(loadingTbodyContent)
      val f = Ajax.get(loadUrl)
      f.onComplete(_ => loading = js.undefined)
      f.onSuccess {
        case req: dom.XMLHttpRequest =>
          val content = upickle.default.read[List[rest.Received]](req.responseText)
          messagesTbody.innerHTML = ""
          content.sortBy(_.timestamp).foreach { rcv =>
            messagesTbody.appendChild(messageRow(rcv).render)
          }
      }
      f.onFailure {
        case _ =>
          messagesTbody.innerHTML = ""
          messagesTbody.appendChild(
            tr(td(colspan := 3, "Unable to download archive, please check server is connected to Cassandra.")).render
          )
      }
      loading = f
    }
  }

  private[this] def messageRow(rcv: rest.Received): Seq[Frag] = {
    val date = new Date(rcv.timestamp.toDouble)
    Seq(
      tr(
        td(FrontendUtil.shortActorName(rcv.from)),
        td(rcv.direction),
        td(FrontendUtil.shortActorName(rcv.to)),
        td(date.toISOString())
      ),
      tr(
        td(colspan := 4)(pre(prettyPrintJson(rcv.payload)))
      )
    )
  }

  private[this] val refreshButton = a(
    cls := "btn btn-default", href := "#", role := "button", float.right,
    span(
      `class` := "imgbtn glyphicon glyphicon-refresh", " "
    ),
    onclick := { () =>
      loadBetween()
    },
    "Refresh view"
  )

  private[this] val loadingTbodyContent = tr(td(colspan := 4, "Loading...")).render
  private[this] val messagesTbody = tbody(loadingTbodyContent).render
  private[this] val rendered = div(
    cls := "panel-body",
    div(
      refreshButton,
      clear.both
    ),
    div(cls := "panel-body", id := "messagespanelbody",
      table(
        cls := "table table-striped table-hover",
        thead(
          tr(th("From"), th(""), th("To"), th("Time"))
        ), messagesTbody
      ))
  ).render

  override def onCreate(): Unit = {
    tabBody.appendChild(rendered)

    loadBetween()
  }
}
