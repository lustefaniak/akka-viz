package akkaviz.frontend.components

import akkaviz.frontend.ActorRepository.ActorState
import akkaviz.frontend.components.GraphView.{AddNode, RemoveNode}
import akkaviz.frontend.{ScheduledQueue, vis}
import org.scalajs.dom._
import rx.Var

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|._

class GraphView(
    showUnconnected: Var[Boolean],
    actorSelectionToggler: (String) => Unit,
    renderNode: (String, ActorState) => vis.Node
) extends Component with GraphViewSettings {

  private[this] val registeredActors = js.Dictionary[Var[ActorState]]()
  private[this] val connectedActors = js.Dictionary[Unit]()
  private[this] val createdLinks = js.Dictionary[Unit]()

  private[this] val currentlyVisibleNodes = js.Dictionary[Unit]()

  private[this] val networkNodes = new vis.DataSet[vis.Node]()
  private[this] val networkEdges = new vis.DataSet[vis.Edge]()
  private[this] var network: js.UndefOr[vis.Network] = js.undefined
  private[this] val scheduler = new ScheduledQueue[GraphView.GraphOperation](applyGraphOperations)
  private[this] lazy val fitOnce = network.foreach(_.fit()) // do it once

  override def attach(parent: Element): Unit = {
    network.foreach(_.destroy())

    val data = vis.NetworkData(networkNodes, networkEdges)
    val n = new vis.Network(parent, data, graphSettings)
    n.onDoubleClick {
      (event: vis.ClickEvent) =>
        event.nodes.foreach(actorSelectionToggler)
    }

    network = n
  }

  def addLink(sender: String, receiver: String): Unit = {
    val linkId = s"${sender}->${receiver}"
    if (!createdLinks.contains(linkId)) {
      createdLinks.update(linkId, ())
      scheduler.enqueueOperation(GraphView.AddLink(sender, receiver, linkId))
      markConnected(sender)
      markConnected(receiver)
    }
  }

  def addActor(node: String, state: Var[ActorState]): Unit = {
    if (!registeredActors.contains(node)) {
      registeredActors.update(node, state)
      state.foreach {
        updatedState =>
          redrawActor(node, updatedState)
      }
    }
  }

  showUnconnected.triggerLater {
    val show = showUnconnected.now
    if (show) {
      registeredActors
        .filterKeys(node => !currentlyVisibleNodes.contains(node))
        .foreach {
          case (node, state) =>
            console.log(s"redraw ${node}")
            redrawActor(node, state.now)
        }
    } else {
      currentlyVisibleNodes.keys.filterNot(connectedActors.contains).foreach {
        case node =>
          console.log(s"remove ${node}")
          scheduler.enqueueOperation(RemoveNode(node))
      }
    }
  }

  private[this] def applyGraphOperations(operationsToApply: js.Array[GraphView.GraphOperation]): Unit = {

    val nodesToAdd = js.Dictionary[vis.Node]()
    val nodesToUpdate = js.Dictionary[vis.Node]()
    val nodesToRemove = js.Dictionary[Unit]()
    val linksToCreate = js.Array[vis.Edge]()

    operationsToApply.foreach {
      case GraphView.AddNode(node, data) =>
        nodesToRemove.delete(node)
        if (currentlyVisibleNodes.contains(node))
          nodesToUpdate.update(node, data)
        else
          nodesToAdd.update(node, data)
      case GraphView.RemoveNode(node) =>
        nodesToAdd.delete(node)
        nodesToUpdate.delete(node)
        nodesToRemove.update(node, ())
      case GraphView.AddLink(from, to, linkId) =>
        linksToCreate.push(vis.Edge(linkId, from, to))
    }

    val removeIds = nodesToRemove.keys.toJSArray
    networkNodes.remove(removeIds)
    removeIds.foreach(currentlyVisibleNodes.delete(_))
    networkNodes.update(nodesToUpdate.values.toJSArray)
    networkNodes.add(nodesToAdd.values.toJSArray)
    nodesToAdd.keys.foreach(currentlyVisibleNodes.update(_, ()))

    networkEdges.add(linksToCreate)

    fitOnce

  }

  private[this] def markConnected(node: String): Unit = {
    connectedActors.update(node, ())
    registeredActors.get(node).foreach {
      state =>
        redrawActor(node, state.now)
    }
  }

  private[this] def redrawActor(node: String, state: ActorState): Unit = {
    if (showUnconnected.now || connectedActors.contains(node)) {
      scheduler.enqueueOperation(AddNode(node, renderNode(node, state)))
    }
  }

}

case object GraphView {

  sealed trait GraphOperation

  case class AddLink(from: String, to: String, linkId: String) extends GraphOperation

  case class AddNode(node: String, data: vis.Node) extends GraphOperation

  case class RemoveNode(node: String) extends GraphOperation

}
