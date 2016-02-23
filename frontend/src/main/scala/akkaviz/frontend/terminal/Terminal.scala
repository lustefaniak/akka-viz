package akkaviz.frontend.terminal

import org.scalajs.dom._

import scala.scalajs.js

@js.native
class Terminal(opts: TerminalOptions = js.native) extends js.Any {

  def on[T](event: String, handler: js.ThisFunction): Unit = js.native

  def destroy(): Unit = js.native

  def open(elem: Element): Unit = js.native

  def options: TerminalOptions = js.native

  def write(s: String): Unit = js.native

  def off(event: String): Unit = js.native

}
