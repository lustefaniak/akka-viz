package akkaviz.frontend.components

import akkaviz.frontend.FrontendUtil
import org.querki.jquery.JQueryStatic
import org.scalajs.dom._

import scala.scalajs.js.Dictionary
import scalatags.JsDom.all._

class HierarchyPanel(detailsOpener: (String) => Unit) extends Component {

  private[this] val $ = JQueryStatic

  private[this] val seenActors: Dictionary[Unit] = Dictionary()

  private[this] val ActorAttr = "actor-path".attr

  private[this] val hierarchy = div(
    cls := "actor-tree",
    ActorAttr := "root",
    ul()
  ).render

  private[this] def node(actorRef: String) = li(
    ActorAttr := actorRef,
    span(
      i(cls := "material-icons"),
      shortName(actorRef) + " ",
      title := actorRef,
      "data-target".attr := s"""[actor-path="$actorRef"]>ul""",
      "data-toggle".attr := "collapse",
      onclick := { () => nodeClicked(actorRef) }
    ),
    a(
      "(details)",
      href := "#",
      onclick := { () => detailsOpener(actorRef) }
    ),
    ul(
      cls := "collapse"
    )
  )

  private[this] def nodeClicked(ref: String) = {
    val node = $(s"""[actor-path="$ref"]""")
    val nodeUl = node.find("ul").first
    val isEmpty = nodeUl.find("ul").first.length == 0
    if (!isEmpty) {
      val isExpanded = node.find("ul").first.hasClass("in")
      if (isExpanded)
        node.find("span>i").first.text("keyboard_arrow_right")
      else
        node.find("span>i").first.text("keyboard_arrow_down")
    }
  }

  private[this] def shortName(ref: String) = {
    ref.stripPrefix("akka://").split("/").lastOption.getOrElse("")
  }

  def insert(ref: String): Unit = innerInsert(ref.stripSuffix("/"))

  def insert(refs: Iterable[String]): Unit = refs.foreach(insert)

  private[this] def innerInsert(ref: String): Unit = {
    if (!exists(ref)) {
      val parentRef = FrontendUtil.parent(ref)
      parentRef.foreach(innerInsert)
      insertSorted(parentRef.getOrElse("root"), ref)
      seenActors.update(ref, ())
    }
  }

  private[this] def insertSorted(parentRef: String, ref: String): Unit = {
    $(s"""[actor-path="$parentRef"]>ul""").toArray.headOption.map {
      parentUl =>
        val siblings = $(s"""[actor-path="$parentRef"]>ul>li""").toArray.toList
        val nextElementOpt = siblings.dropWhile(_.getAttribute("actor-path") < ref).headOption
        val newNode = node(ref).render
        nextElementOpt match {
          case Some(elem) => parentUl.insertBefore(newNode, elem)
          case None => parentUl.appendChild(newNode)
        }
        $(parentUl).parent.find("span>i").first.text("keyboard_arrow_right")
    }
  }

  private[this] def exists(ref: String) = seenActors.contains(ref)

  override def attach(parent: Element): Unit = parent.appendChild(hierarchy)

}
