package akkaviz.frontend.components

import akkaviz.frontend.vis
import akkaviz.frontend.vis.{NetworkData, NetworkOptions}
import org.scalajs.dom.html.Element
import org.scalajs.dom.{Element => domElement}
import scala.collection.breakOut

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

class FsmGraph(parent: Element) {

  private[this] val networkNodes = new vis.DataSet[vis.Node]()
  private[this] val networkEdges = new vis.DataSet[vis.Edge]()
  private[this] val data = NetworkData(networkNodes, networkEdges)
  private[this] val network = new vis.Network(parent, data, fsmNetworkOptions)

  network.on("stabilizationProgress", () => network.fit())
  network.on("stabilized", () => network.fit())

  def displayFsm(transitions: Set[(String, String)]): Unit = {
    if (transitions.isEmpty) {
      parent.style.display = "none"
    } else {
      parent.style.display = "box"

      val allStates = transitions.flatMap { case (from, to) => Set(from, to) }
      val nodes: js.Array[vis.Node] = allStates.map(state => vis.Node(state, simplifyStateName(state), state))(breakOut)
      val edges: js.Array[vis.Edge] = transitions.map {
        case (from, to) => vis.Edge(s"${from}->${to}", from, to)
      }(breakOut)
      networkNodes.clear()
      networkNodes.add(nodes)
      networkEdges.clear()
      networkEdges.add(edges)
    }
  }

  @inline
  private[this] def simplifyStateName(state: String): String = {
    state.split('.').last.stripSuffix("$")
  }

  @inline
  private[this] def fsmNetworkOptions: NetworkOptions = {
    val opts = js.Dynamic.literal()

    opts.nodes = js.Dynamic.literal()
    opts.nodes.shape = "dot"
    opts.physics = js.Dynamic.literal()
    opts.physics.solver = "forceAtlas2Based"
    opts.physics.forceAtlas2Based = js.Dynamic.literal()
    opts.physics.forceAtlas2Based.springLength = 100

    opts.asInstanceOf[NetworkOptions]
  }

}
