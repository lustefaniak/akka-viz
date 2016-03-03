package akkaviz.frontend

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

@js.native
object DOMGlobalScope extends js.GlobalScope {
  def $: js.Dynamic = js.native

  def ab2str(ab: ArrayBuffer): String = js.native

  def str2ab(str: String): ArrayBuffer = js.native

}
