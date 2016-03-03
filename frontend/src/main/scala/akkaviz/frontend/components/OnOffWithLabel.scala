package akkaviz.frontend.components

import org.scalajs.dom.{Element => domElement}

import scalatags.JsDom.all._

trait OnOffWithLabel {
  lazy val lbl = span().render
  lazy val inp = input(tpe := "checkbox").render

  lazy val stateBtn = div(
    `class` := "togglebutton",
    label(
      inp, lbl
    )
  )
}

