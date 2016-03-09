package akkaviz.frontend.components

import akkaviz.frontend.ActorRepository.ActorState
import akkaviz.frontend.FrontendUtil
import akkaviz.protocol
import org.querki.jquery.JQueryStatic
import org.scalajs.dom._
import rx.Var

import scala.scalajs.js.Dictionary
import scalatags.JsDom.all._

class HierarchyPanel(
  currentActorState: (String) => Var[ActorState],
  upstreamSend: protocol.ApiClientMessage => Unit
)
    extends Component {

  private val $ = JQueryStatic

  val seenActors: Dictionary[Unit] = Dictionary()

  private val ActorAttr = "actor-path".attr

  val hierarchy = div(
    cls := "actor-tree",
    ActorAttr := "root",
    ul()
  ).render

  private def node(ref: String) = li(
    ActorAttr := ref,
    span(
      i(cls := "glyphicon glyphicon-leaf"),
      shortName(ref),
      title := ref,
      "data-target".attr := s"""[actor-path="$ref"]>ul""",
      "data-toggle".attr := "collapse",
      onclick := { () => nodeClicked(ref) }
    ),
    a(
      "(details)",
      href := "#",
      onclick := { () => openTab(ref) }
    ),
    ul(
      cls := "collapse"
    )
  )

  def nodeClicked(ref: String) = {
    val node = $(s"""[actor-path="$ref"]""")
    val nodeUl = node.find("ul").first
    val isEmpty = nodeUl.find("ul").first.length == 0
    if (!isEmpty) {
      val isExpanded = node.find("ul").first.hasClass("in")
      if (isExpanded)
        node.find("span>i").first.removeClass("glyphicon-minus").addClass("glyphicon-plus")
      else
        node.find("span>i").first.removeClass("glyphicon-plus").addClass("glyphicon-minus")
    }
  }

  def openTab(ref: String): Unit = {
    val stateVar = currentActorState(ref)
    val tab: ActorStateTab = new ActorStateTab(stateVar, upstreamSend)
    tab.attach(document.querySelector("#right-pane"))
  }

  private def shortName(ref: String) = {
    ref.stripPrefix("akka://").split("/").last
  }

  def insert(ref: String): Unit = innerInsert(ref.stripSuffix("/"))

  def insert(refs: Iterable[String]): Unit = refs.foreach(insert)

  private def innerInsert(ref: String): Unit = {
    if (!exists(ref)) {
      val parentRef = FrontendUtil.parent(ref)
      parentRef.foreach(innerInsert)
      insertSorted(parentRef.getOrElse("root"), ref)
      seenActors.update(ref, ())
    }
  }

  private def insertSorted(parentRef: String, ref: String): Unit = {
    val parentUl = $(s"""[actor-path="$parentRef"]>ul""").toArray.head
    val siblings = $(s"""[actor-path="$parentRef"]>ul>li""").toArray.toList
    val nextElementOpt = siblings.dropWhile(_.getAttribute("actor-path") < ref).headOption
    val newNode = node(ref).render
    nextElementOpt match {
      case Some(elem) => parentUl.insertBefore(newNode, elem)
      case None       => parentUl.appendChild(newNode)
    }
    $(parentUl).parent.find("span>i").first.removeClass("glyphicon-leaf").addClass("glyphicon-plus")
  }

  private def exists(ref: String) = seenActors.contains(ref)

  override def attach(parent: Element): Unit = parent.appendChild(hierarchy)
}
