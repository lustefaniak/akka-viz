package akkaviz.frontend.components

import akkaviz.frontend.DOMGlobalScope._
import akkaviz.frontend.FrontendUtil
import org.scalajs.dom.Element

import scala.scalajs.js.Dictionary
import scalatags.JsDom.all._

class HierarchyPanel extends Component {

  val seenActors: Dictionary[Unit] = Dictionary()

  private val ActorAttr = "actor-path".attr

  val hierarchy = div(
    ActorAttr := "root",
    ul())

  private def node(ref: String) = li(
    ActorAttr := ref,
    span(
      shortName(ref),
      title := ref,
      "data-target".attr := s"""[actor-path="$ref"]>ul""",
      "data-toggle".attr := "collapse"
    ),
    ul(
      cls := "collapse"
    )
  )

  private def shortName(ref: String) = {
    ref.stripPrefix("akka://").split("/").last
  }

  def insert(ref: String): Unit = {
    if (!exists(ref)) {
      val parentRef = FrontendUtil.parent(ref)
      parentRef.foreach(insert)
      insertSorted(parentRef.getOrElse("root"), ref)
      seenActors.update(ref, ())
    }
  }

  private def insertSorted(parentRef: String, ref: String): Unit = {
    val siblings = $(s"""[actor-path="$parentRef"]>ul>li""").asInstanceOf[scala.scalajs.js.Array[scala.scalajs.js.Any]]
    val nextElementOpt = siblings.toList.dropWhile(s => $(s).attr("actor-path").asInstanceOf[String] < ref).headOption
    nextElementOpt match {
      case Some(elem) => $(elem).before(node(ref).render)
      case None => $(s"""[actor-path="$parentRef"]>ul""").append(node(ref).render)
    }
  }

  private def exists(ref: String) = seenActors.contains(ref)

  override def attach(parent: Element): Unit = parent.appendChild(hierarchy.render)
}
