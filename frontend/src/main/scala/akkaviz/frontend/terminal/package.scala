package akkaviz.frontend

import scala.scalajs.js

package object terminal {

  implicit class PimpedTerminal(underlying: Terminal) {
    def onData(fn: js.ThisFunction1[Any, String, Unit]): Unit = underlying.on("data", fn)

    def onTitle(fn: js.ThisFunction1[Any, String, Unit]): Unit = underlying.on("title", fn)
  }

}