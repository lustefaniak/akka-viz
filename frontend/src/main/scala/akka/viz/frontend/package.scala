package akka.viz

import org.scalajs.dom._
import org.scalajs.dom.raw.Element

package object frontend {

  implicit class RichElement(elem: Element) {
    def onClick(f: () => Any): Unit = {
      elem.addEventListener("click", { (e: Event) => f() }, true)
    }

    def onEnter(f: () => Any): Unit = {
      elem.addEventListener("keydown", { (e: KeyboardEvent) =>
        val enterKeyCode = 13
        if (e.keyCode == enterKeyCode)
          f()
      }, true)
    }
  }

}
