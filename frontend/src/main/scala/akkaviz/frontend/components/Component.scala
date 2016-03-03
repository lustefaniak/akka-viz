package akkaviz.frontend.components

import org.scalajs.dom.{Element => domElement}

trait Component {
  def attach(parent: domElement): Unit
}
