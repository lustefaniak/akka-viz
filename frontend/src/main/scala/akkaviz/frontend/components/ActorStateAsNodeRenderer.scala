package akkaviz.frontend.components

import akkaviz.frontend.ActorRepository.ActorState
import akkaviz.frontend.vis
import org.scalajs.dom.console
import scala.scalajs.js

object ActorStateAsNodeRenderer {

  def render(id: String, state: ActorState): vis.Node = {
    js.Dynamic.literal(
      id = id,
      title = id,
      label = state.label,
      color = if (state.isDead) deadColor else colorForString(state.system)
    ).asInstanceOf[vis.Node]
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
  private[this] val deadColor = "#666666"

  @inline
  private[this] def colorForString(str: String): String = {
    possibleColors(Math.abs(str.hashCode) % possibleColors.size)
  }

}
