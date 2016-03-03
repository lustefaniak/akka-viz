package akkaviz.frontend.components

import akkaviz.frontend.vis.NetworkOptions
import akkaviz.frontend.{DOMGlobalScope, ScheduledQueue, vis}
import org.scalajs.dom._
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
    val data = vis.NetworkData(networkNodes, networkEdges)
    network.foreach(_.destroy())
    network = new vis.Network(parent, data, graphSettings)
  }

  private def graphSettings: NetworkOptions = {
    //TODO: Generate good parameters using http://visjs.org/examples/network/other/configuration.html
    val opts = js.Dynamic.literal()

    //for faster rendering
    //opts.edges = js.Dynamic.literal(smooth = js.Dynamic.literal(`type` = "continuous"))

    opts.nodes = js.Dynamic.literal()
    opts.nodes.shape = "dot"
    opts.nodes.scaling = js.Dynamic.literal()
    opts.nodes.scaling.min = 10
    opts.nodes.scaling.max = 30
    opts.nodes.scaling.label = js.Dynamic.literal()
    opts.nodes.scaling.label.min = 8
    opts.nodes.scaling.label.max = 30
    opts.nodes.scaling.label.drawThreshold = 10
    opts.nodes.scaling.label.maxVisible = 30

    opts.interaction = js.Dynamic.literal()
    opts.interaction.hover = true
    opts.interaction.navigationButtons = true
    opts.interaction.tooltipDelay = 200
    opts.interaction.hideEdgesOnDrag = true

    opts.physics = js.Dynamic.literal()
    //opts.physics.solver = "hierarchicalRepulsion"
    //opts.physics.hierarchicalRepulsion = js.Dynamic.literal()
    //opts.physics.hierarchicalRepulsion.centralGravity = 2.0
    //opts.physics.hierarchicalRepulsion.nodeDistance = 200
    opts.physics.solver = "forceAtlas2Based"
    opts.physics.forceAtlas2Based = js.Dynamic.literal()
    opts.physics.forceAtlas2Based.springLength = 100

    opts.configure = js.Dynamic.literal()
    opts.configure.enabled = true
    opts.configure.container = document.getElementById("graphsettings")

    console.log(opts)
    opts.asInstanceOf[NetworkOptions]
  }

  private[this] val scheduler = new ScheduledQueue[GraphView.GraphOperation](applyGraphOperations)
  private[this] val nodeData = js.Dictionary[String]()
  private[this] val connectedNodes = js.Dictionary[Unit]()
  private[this] val createdLinks = js.Dictionary[Unit]()
  private[this] lazy val fitOnce = network.foreach(_.fit()) // do it once

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

  //http://tools.medialab.sciences-po.fr/iwanthue/index.php
  private[this] val possibleColors = js.Array[String](
    "#D7C798",
    "#8AD9E5",
    "#ECACC1",
    "#97E3B0",
    "#D5EB85",
    "#F2AE99",
    "#DFC56D",
    "#DAECD0",
    "#D2DCE6",
    "#D4F0AD",
    "#D9C0B3",
    "#E8AD70",
    "#DAC1E1",
    "#82DDC9",
    "#B8BE74",
    "#A8C5AA",
    "#A7C5E2",
    "#A0D38F",
    "#EEDF98",
    "#ADD7D4"
  )

  @inline
  private def colorForNode(node: String): String = {
    val system = node.split('/')(2)

    possibleColors(Math.abs(system.hashCode) % (possibleColors.size - 1))
  }

  private def applyGraphOperations(operationsToApply: js.Array[GraphView.GraphOperation]): Unit = {

    val nodesToAdd = js.Dictionary[vis.Node]()
    val nodesToRemove = js.Dictionary[Unit]()
    val linksToCreate = js.Array[vis.Edge]()

    operationsToApply.foreach {
      case GraphView.AddNode(node, label) =>
        nodesToRemove.delete(node)
        if (!visibleNodes.contains(node))
          nodesToAdd.update(node, vis.Node(node, label, color = colorForNode(node)))
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
