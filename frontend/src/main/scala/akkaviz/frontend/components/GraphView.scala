package akkaviz.frontend.components

import akkaviz.frontend.{DOMGlobalScope, ScheduledQueue, vis}
import org.scalajs.dom.{Element, console}
import rx.Var

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|
import scala.scalajs.js.|._

class GraphView(showUnconnected: Var[Boolean]) extends Component {

  private[this] lazy val visibleNodes = js.Dictionary[Unit]()
  private[this] lazy val networkNodes = new vis.DataSet[vis.Node]()
  private[this] lazy val networkEdges = new vis.DataSet[vis.Edge]()
  private[this] var network: js.UndefOr[vis.Network] = js.undefined

  override def attach(parent: Element): Unit = {
    console.log(networkNodes)
    console.log(networkEdges)
    console.log(parent)
    val data = vis.NetworkData(networkNodes, networkEdges)
    console.log(data)
    network.foreach(_.destroy())
    network = new vis.Network(parent, data, vis.NetworkOptions())
    console.log(network)
  }

  private[this] val scheduler = new ScheduledQueue[GraphView.GraphOperation](applyGraphOperations)
  private[this] val nodeData = js.Dictionary[String]()
  private[this] val connectedNodes = js.Dictionary[Unit]()
  private[this] val createdLinks = js.Dictionary[Unit]()
  private[this] lazy val fitOnce = network.foreach(_.fit) // do it once

  private[this] def isNodeConnected(node: String): Boolean = {
    connectedNodes.contains(node)
  }

  showUnconnected.triggerLater {
    val show = showUnconnected.now
    if (show) {
      nodeData.foreach {
        case (node, label) =>
          scheduler.enqueueOperation(GraphView.AddNode(node, label))
      }
    } else {
      nodeData.foreach {
        case (node, label) if !isNodeConnected(node) =>
          scheduler.enqueueOperation(GraphView.RemoveNode(node))
        case _ => //do nothing
      }
    }
  }

  private def applyGraphOperations(operationsToApply: js.Array[GraphView.GraphOperation]): Unit = {

    val nodesToAdd = js.Dictionary[vis.Node]()
    val nodesToRemove = js.Dictionary[Unit]()
    val linksToCreate = js.Array[vis.Edge]()

    operationsToApply.foreach {
      case GraphView.AddNode(node, label) =>
        nodesToRemove.delete(node)
        if (!visibleNodes.contains(node))
          nodesToAdd.update(node, vis.Node(node, label)) //, color = DOMGlobalScope.colorByHashCode(node)))
      case GraphView.RemoveNode(node) =>
        nodesToAdd.delete(node)
        nodesToRemove.update(node, ())
      case GraphView.AddLink(from, to, linkId) =>
        linksToCreate.push(vis.Edge(linkId, from, to))
    }

    val removeIds = nodesToRemove.keys.toJSArray
    networkNodes.remove(removeIds)
    removeIds.foreach(visibleNodes.delete(_))
    networkNodes.add(nodesToAdd.values.toJSArray)
    nodesToAdd.keys.foreach(visibleNodes.update(_, ()))

    networkEdges.add(linksToCreate)

    fitOnce

  }

  def ensureGraphLink(sender: String, receiver: String, nodesLabeler: (String) => String): Unit = {
    val linkId = s"${sender}->${receiver}"
    if (!createdLinks.contains(linkId)) {
      createdLinks.update(linkId, ())
      ensureNodeExists(sender, nodesLabeler(sender))
      ensureNodeExists(receiver, nodesLabeler(receiver))
      connectedNodes.update(sender, ())
      connectedNodes.update(receiver, ())
      scheduler.enqueueOperation(GraphView.AddLink(sender, receiver, linkId))
    }
  }

  def ensureNodeExists(node: String, label: String): Unit = {
    nodeData.update(node, label)
    if (showUnconnected.now || isNodeConnected(node))
      scheduler.enqueueOperation(GraphView.AddNode(node, label))
  }

}

case object GraphView {

  sealed trait GraphOperation

  case class AddLink(from: String, to: String, linkId: String) extends GraphOperation

  case class AddNode(node: String, label: String) extends GraphOperation

  case class RemoveNode(node: String) extends GraphOperation

}
