package akkaviz.frontend.components

import akkaviz.frontend.FrontendUtil
import org.querki.jquery.JQueryStatic
import org.scalajs.dom.Element

import scala.scalajs.js.Dictionary
import scalatags.JsDom.all._

class HierarchyPanel extends Component {

  private val $ = JQueryStatic

  val seenActors: Dictionary[Unit] = Dictionary()

  private val ActorAttr = "actor-path".attr

  val hierarchy = div(
    ActorAttr := "root",
    ul()).render

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
    val parentUl = $(s"""[actor-path="$parentRef"]>ul""").toArray.head
    val siblings = $(s"""[actor-path="$parentRef"]>ul>li""").toArray
    val nextElementOpt = siblings.toList.dropWhile(_.getAttribute("actor-path") < ref).headOption
    nextElementOpt match {
      case Some(elem) => parentUl.insertBefore(node(ref).render, elem)
      case None => parentUl.appendChild(node(ref).render)
    }
  }

  private def exists(ref: String) = seenActors.contains(ref)

  override def attach(parent: Element): Unit = parent.appendChild(hierarchy)
}
