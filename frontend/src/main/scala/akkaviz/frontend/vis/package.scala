package akkaviz.frontend

import org.scalajs.dom.{Element, console}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

package object vis {

  @js.native
  trait Node extends js.Any {
    def id: js.Any

  }

  object Node {
    def apply(id: String, label: String, value: js.UndefOr[Int] = js.undefined, color: js.UndefOr[String] = js.undefined): vis.Node = {
      js.Dynamic.literal(id = id, label = label, value = value, color = color).asInstanceOf[vis.Node]
    }
  }

  @js.native
  trait Edge extends js.Any {
    def id: js.Any
  }

  object Edge {
    def apply(id: String, from: String, to: String, arrows: js.UndefOr[String] = "to", title: js.UndefOr[String] = js.undefined, value: js.UndefOr[Int] = js.undefined): vis.Edge = {
      js.Dynamic.literal(id = id, from = from, to = to, value = value, arrows = arrows).asInstanceOf[vis.Edge]
    }
  }

  @js.native
  trait DataSetOptions extends js.Any {

  }

  @js.native
  @JSName("vis.DataSet")
  class DataSet[T](data: js.Array[T] = js.Array[T](), options: js.UndefOr[DataSetOptions] = js.native) extends js.Any {

    def add(d: T): js.Array[String] = js.native

    def add(d: js.Array[T]): js.Array[String] = js.native

    def remove(d: String): js.Array[String] = js.native

    def remove(d: js.Array[String]): js.Array[String] = js.native

    def get(d: String): T = js.native

    def get(d: js.Array[String]): js.Array[T] = js.native

    def setOptions(o: DataSetOptions): Unit = js.native

  }

  @js.native
  trait NetworkData extends js.Any {
    var nodes: DataSet[Node] = js.native
    var edges: DataSet[Edge] = js.native
  }

  @js.native
  trait NetworkOptions extends js.Any {

  }

  //TOD: Generate good parameters using http://visjs.org/examples/network/other/configuration.html
  object NetworkOptions {
    def apply(): NetworkOptions = {

      val opts = js.Dynamic.literal()

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

      opts.physics.forceAtlas2Based = js.Dynamic.literal()
      opts.physics.forceAtlas2Based.springLength = 100
      opts.physics.minVelocity = 0.75
      opts.physics.solver = "forceAtlas2Based"

      opts.configure = js.Dynamic.literal()
      opts.configure.enabled = true
      opts.configure.container = document.getElementById("graphsettings")

      console.log(opts)
      opts.asInstanceOf[NetworkOptions]
    }
  }

  @js.native
  @JSName("vis.Network")
  class Network(element: Element, data: NetworkData, options: NetworkOptions) extends js.Any {

  }

}
