package akkaviz.frontend.components

import akkaviz.frontend.vis.NetworkOptions
import org.scalajs.dom._

import scala.scalajs.js

trait GraphViewSettings {
  def graphSettings: NetworkOptions = {
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
    opts.interaction.hideEdgesOnDrag = true
    opts.interaction.multiselect = true

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
}
