package akkaviz.frontend.components

import akkaviz.frontend.FrontendUtil
import org.querki.jquery.JQueryStatic
import org.scalajs.dom._

import scala.scalajs.js.Dictionary
import scalatags.JsDom.all._

class HierarchyPanel(detailsOpener: (String) => Unit) extends Component {

  private[this] val $ = JQueryStatic

  private[this] val indentPx = 15
  private[this] val arrowRight = "keyboard_arrow_right"
  private[this] val arrowDown = "keyboard_arrow_down"
  private[this] val actorAttr = "actor-path".attr

  private[this] val seenActors: Dictionary[Unit] = Dictionary()

  private[this] val hierarchy = div(
    cls := "actor-tree list-group",
    actorAttr := "root"
  ).render

  private[this] def depth(actorRef: String): Int = actorRef.count(_ == '/') - 2

  private[this] def node(actorRef: String) = a(
    actorAttr := actorRef,
    cls := "list-group-item",
    href := "#",
    paddingLeft := (indentPx * depth(actorRef)).px,
    title := actorRef,
    "data-target".attr := s"""[actor-path="$actorRef"].list-group""",
    "data-toggle".attr := "collapse",
    onclick := { () => nodeClicked(actorRef) },
    i(cls := "material-icons"),
    lastPathElement(actorRef) + " ",
    a(
      "(details)",
      href := "#",
      onclick := { (event: Event) =>
        event.stopPropagation()
        detailsOpener(actorRef)
      }
    )
  )

  private[this] def nodeClicked(ref: String) = {
    val itemClicked = findItem(ref)
    val group = findGroup(ref)
    val isEmpty = group.children().length == 0
    val isCollapsing = group.hasClass("collapsing")
    if (!isEmpty && !isCollapsing) {
      val isExpanded = group.hasClass("in")
      if (isExpanded)
        itemClicked.find("i.material-icons").first.text(arrowRight)
      else {
        itemClicked.find("i.material-icons").first.text(arrowDown)
      }
    }
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
    val parentQuery = findGroup(parentRef)
    parentQuery.toArray.headOption.map {
      parent =>
        val siblings = parentQuery.children(".list-group-item").toArray.toList
        val nextElementOpt = siblings.dropWhile(_.getAttribute("actor-path") < ref).headOption
        val newNode = node(ref).render
        nextElementOpt match {
          case Some(elem) => parent.insertBefore(newNode, elem)
          case None =>
            parent.appendChild(newNode)
            findItem(parentRef).find("i.material-icons").first.text(arrowRight)
        }
        $(newNode).after(listGroupElement(ref))
    }
  }

  private[this] def listGroupElement(ref: String) =
    div(cls := "list-group collapse", actorAttr := ref).render

  private[this] def lastPathElement(ref: String) =
    ref.stripPrefix("akka://").split("/").lastOption.getOrElse("")

  private[this] def findGroup(actorRef: String) =
    $(s"""[actor-path="$actorRef"].list-group""")

  private[this] def findItem(actorRef: String) =
    $(s"""[actor-path="$actorRef"].list-group-item""")

  private[this] def exists(ref: String) = seenActors.contains(ref)

  override def attach(parent: Element): Unit = parent.appendChild(hierarchy)
}
