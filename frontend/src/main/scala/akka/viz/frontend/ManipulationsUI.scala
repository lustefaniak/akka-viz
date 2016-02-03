package akka.viz.frontend

import org.scalajs.dom.Element
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.Event
import rx.Var

import scala.scalajs.js.ThisFunction1
import scalatags.JsDom.all._

trait ManipulationsUI {
  lazy val delaySlider = input(id := "delay-slider", tpe := "range", min := 0, max := 2000, step := 100, value := 0, onchange := handleSliderChange).render
  lazy val delayDisplay = span("0 msec").render

  val receiveDelayPanel = {
    div(`class` := "panel panel-default",
      div(`class` := "panel-heading", p("Receive delay")),
      div(`class` := "panel-body",
        delaySlider, delayDisplay,
        p("Warning: can cause TimeoutException! Use with care!")
      )
    )
  }

  val delayMillis: Var[Int] = Var(0)

  def handleSliderChange: ThisFunction1[Element, Event, Unit] = { (self: Element, event: Event) =>
    delayMillis() = self.asInstanceOf[Input].valueAsNumber
  }

  delayMillis.trigger{
    val millis: Int = delayMillis.now
    delaySlider.value = millis.toString
    delayDisplay.innerHTML = s"$millis msec"
  }
}
