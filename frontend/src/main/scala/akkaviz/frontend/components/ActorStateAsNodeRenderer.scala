package akkaviz.frontend.components

import akkaviz.frontend.ActorRepository.ActorState
import akkaviz.frontend.{FancyColors, vis}

import scala.scalajs.js

object ActorStateAsNodeRenderer extends FancyColors {

  def render(id: String, state: ActorState): vis.Node = {
    js.Dynamic.literal(
      id = id,
      title = id,
      label = state.label,
      color = if (state.isDead) deadColor else colorForString(state.system)
    ).asInstanceOf[vis.Node]
  }

}
