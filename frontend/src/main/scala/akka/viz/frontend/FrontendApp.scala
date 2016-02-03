package akka.viz.frontend

import akka.viz.protocol._
import org.scalajs.dom.html.Input
import org.scalajs.dom.{onclick => _, raw => _, _}
import rx._
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.{ThisFunction0, JSApp, JSON}
import scala.util.Try
import scalatags.JsDom.all._
import upickle.default._

case class FsmTransition(fromStateClass: String, toStateClass: String)

object FrontendApp extends JSApp with FrontendUtil with Persistence
    with MailboxDisplay with PrettyJson {

  val createdLinks = scala.collection.mutable.Set[String]()
  val graph = DOMGlobalScope.graph

  val _actorClasses: mutable.Map[String, Var[js.UndefOr[String]]] = mutable.Map()

  def actorClasses(actor: String) = _actorClasses.getOrElseUpdate(actor, Var(js.undefined))

  val fsmTransitions = mutable.Map[String, mutable.Set[FsmTransition]]()
  val currentActorState = mutable.Map[String, String]()

  private def handleDownstream(messageEvent: MessageEvent): Unit = {
    val message: ApiServerMessage = ApiMessages.read(messageEvent.data.asInstanceOf[String])

    message match {
      case rcv: Received =>
        val sender = actorName(rcv.sender)
        val receiver = actorName(rcv.receiver)
        addActorsToSeen(sender, receiver)
        messageReceived(rcv)
        ensureGraphLink(sender, receiver)

      case ac: AvailableClasses =>
        seenMessages() = ac.availableClasses.toSet

      case Spawned(child, parent) =>
        addActorsToSeen(actorName(child), actorName(parent))

      case fsm: FSMTransition =>
        val actor = actorName(fsm.ref)
        //FIXME: subscribe for data
        fsmTransitions.getOrElseUpdate(actor, mutable.Set()) += FsmTransition(fsm.currentStateClass, fsm.nextStateClass)
        currentActorState.update(actor, """{"state": ${fsm.nextState}, "data":${fsm.nextData}}""")

      case i: Instantiated =>
        val actor = actorName(i.ref)
        actorClasses(actor)() = i.clazz

      case CurrentActorState(ref, state) =>
        currentActorState.update(actorName(ref), state)

      case mb: MailboxStatus =>
        handleMailboxStatus(mb)
    }
  }

  private def ensureGraphLink(sender: String, receiver: String): Unit = {
    val linkId = s"${sender}->${receiver}"
    if (!createdLinks(linkId)) {
      createdLinks.add(linkId)
      graph.beginUpdate()
      graph.addLink(sender, receiver, linkId)
      graph.endUpdate()
    }
  }

  val seenActors = Var[Set[String]](Set())
  val selectedActors = persistedVar[Set[String]](Set(), "selectedActors")
  val seenMessages = Var[Set[String]](Set())
  val selectedMessages = persistedVar[Set[String]](Set(), "selectedMessages")

  val addNodesObs = seenActors.trigger {
    seenActors.now.foreach(graph.addNode(_, js.Dictionary.empty[js.Any]))
  }

  private def addActorsToSeen(actorName: String*): Unit = {
    val previouslySeen = seenActors.now
    val newSeen = previouslySeen ++ actorName.filterNot(previouslySeen(_))
    seenActors() = newSeen
  }

  lazy val messagesContent = document.getElementById("messagespanelbody").getElementsByTagName("tbody")(0).asInstanceOf[Element]

  private def messageReceived(rcv: Received): Unit = {
    def insert(e: Element): Unit = {
      messagesContent.insertBefore(e, messagesContent.firstChild)
    }
    val uid = rcv.eventId
    val sender = actorName(rcv.sender)
    val receiver = actorName(rcv.receiver)
    val selected = selectedActors.now

    if (selected.contains(sender) || selected.contains(receiver)) {

      val mainRow = tr(
        "data-toggle".attr := "collapse",
        "data-target".attr := s"#detail$uid",
        td(sender),
        td(receiver),
        td(rcv.payloadClass)
      )

      val payload: String = rcv.payload.getOrElse("")
      val detailsRow = tr(
        id := s"detail$uid",
        `class` := "collapse",
        td(
          colspan := 3,
          div(pre(prettyPrintJson(payload))) // FIXME: display formated lazily
        )
      )

      insert(detailsRow.render)
      insert(mainRow.render)
    }
  }

  @JSExport("toggleActor")
  def toggleActor(actorPath: String): Unit = {
    if (selectedActors.now contains actorPath) {
      console.log(s"Unselected '$actorPath' actor")
      selectedActors() = selectedActors.now - actorPath
    } else {
      console.log(s"Selected '$actorPath' actor")
      selectedActors() = selectedActors.now + actorPath
    }
  }

  def main(): Unit = {
    val upstream = ApiConnection(webSocketUrl("stream"), handleDownstream)

    val popoverContent: ThisFunction0[Element, Node] = (that: Element) => {
      val actor: String = that.getAttribute("data-actor")
      val actorState: String = currentActorState.get(actor).map(prettyPrintJson).getOrElse("Internal state unknown")
      val popover = Seq(
        h5(actor),
        h6("Class: " + actorClasses(actor).now.getOrElse("")),
        pre(actorState)
      )

      val elem = popover.render
      elem
    }

    val popoverOptions = js.Dictionary(
      "content" -> popoverContent,
      "trigger" -> "hover",
      "placement" -> "right",
      "html" -> true
    )

    val actorsObs = Rx.unsafe {
      (seenActors(), selectedActors())
    }.trigger {
      val seen = seenActors.now.toList.sorted
      val selected = selectedActors.now

      val content = seen.map {
        actorName =>
          val isSelected = selected.contains(actorName)
          val element = tr(
            td(input(`type` := "checkbox", if (isSelected) checked else ())),
            td(if (isSelected) b(actorName) else actorName), onclick := {
              () => toggleActor(actorName)
            }
          )(data("actor") := actorName).render

          DOMGlobalScope.$(element).popover(popoverOptions)
          element
      }

      val actorTree = document.getElementById("actortree").getElementsByTagName("tbody")(0).asInstanceOf[Element]
      actorTree.innerHTML = ""
      actorTree.appendChild(content.render)
    }

    val messagesObs = Rx.unsafe {
      (seenMessages(), selectedMessages())
    }.triggerLater {

      val seen = seenMessages.now.toList.sorted
      val selected = selectedMessages.now

      val content = seen.map {
        clazz =>
          val contains = selected(clazz)
          tr(
            td(input(`type` := "checkbox", if (contains) checked else ())),
            td(if (contains) b(clazz) else clazz),
            onclick := {
              () =>
                console.log(s"Toggling ${clazz} now it will be ${!contains}")
                selectedMessages() = if (contains) selected - clazz else selected + clazz
            }
          )
      }

      val messages = document.getElementById("messagefilter").getElementsByTagName("tbody")(0).asInstanceOf[Element]
      messages.innerHTML = ""
      messages.appendChild(content.render)

      console.log(s"Will send allowedClasses: ${selected.mkString("[", ",", "]")}")
      import upickle.default._
      upstream.send(write(SetAllowedMessages(selected.toList)))

      selectedActors.trigger {
        if (selectedActors.now.isEmpty) {
          document.getElementById("messagespaneltitle").innerHTML = s"Select actor to show its messages"
        } else {
          document.getElementById("messagespaneltitle").innerHTML = s"Messages"
        }
        messagesContent.innerHTML = ""
      }
    }

    def clearMessageFilters() = {
      selectedMessages() = Set.empty
    }

    def selectAllMessageFilters() = {
      selectedMessages() = seenMessages.now
    }

    def regexMessageFilter() = {
      val input = document.getElementById("messagefilter-regex").asInstanceOf[Input].value
      Try(input.r).foreach { r =>
        selectedMessages() = seenMessages.now.filter(_.matches(r.regex))
      }
    }

    document.querySelector("a#messagefilter-select-none").onClick(() => clearMessageFilters())
    document.querySelector("a#messagefilter-select-all").onClick(() => selectAllMessageFilters())
    document.getElementById("messagefilter-regex").onEnter(() => regexMessageFilter())

    def clearActorFilters() = {
      selectedActors() = Set.empty
    }

    def selectAllActorFilters() = {
      selectedActors() = seenActors.now
    }

    def regexActorFilter() = {
      val input = document.getElementById("actorfilter-regex").asInstanceOf[Input].value
      Try(input.r).foreach { r =>
        selectedActors() = seenActors.now.filter(_.matches(r.regex))
      }
    }

    document.querySelector("a#actorfilter-select-none").onClick(() => clearActorFilters())
    document.querySelector("a#actorfilter-select-all").onClick(() => selectAllActorFilters())
    document.getElementById("actorfilter-regex").onEnter(() => regexActorFilter())
  }
}