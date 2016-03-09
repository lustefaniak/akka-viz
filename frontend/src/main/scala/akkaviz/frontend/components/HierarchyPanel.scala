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
    ActorAttr := "root",
    ul()
  ).render

  private def node(ref: String) = li(
    ActorAttr := ref,
    span(
      shortName(ref),
      title := ref,
      "data-target".attr := s"""[actor-path="$ref"]>ul""",
      "data-toggle".attr := "collapse"
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

  def openTab(ref: String): Unit = {
    val stateVar = currentActorState(ref)
    val tab: ActorStateTab = new ActorStateTab(stateVar, upstreamSend)
    tab.attach(document.querySelector("#right-pane"))
  }

  private def shortName(ref: String) = {
    ref.stripPrefix("akka://").split("/").last
  }

  def insert(ref: String) = innerInsert(ref.stripSuffix("/"))

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
    nextElementOpt match {
      case Some(elem) => parentUl.insertBefore(node(ref).render, elem)
      case None       => parentUl.appendChild(node(ref).render)
    }
  }

  private def exists(ref: String) = seenActors.contains(ref)

  override def attach(parent: Element): Unit = parent.appendChild(hierarchy)
}
