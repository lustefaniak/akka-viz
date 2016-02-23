package akkaviz.frontend.terminal

import scala.scalajs.js

@js.native
trait TerminalOptions extends js.Any {
  var colors: js.Array[String] = js.native
  var cols: Int = js.native
  var convertEol: Boolean = js.native
  var cursorBlink: Boolean = js.native
  var debug: Boolean = js.native
  var geometry: js.Tuple2[Int, Int] = js.native
  var popOnBell: Boolean = js.native
  var rows: Int = js.native
  var screenKeys: Boolean = js.native
  var scrollback: Int = js.native
  var termName: String = js.native
  var useFocus: Boolean = js.native
  var useStyle: Boolean = js.native
  var visualBell: Boolean = js.native
}
