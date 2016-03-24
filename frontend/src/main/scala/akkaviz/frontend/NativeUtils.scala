package akkaviz.frontend

import scala.scalajs.js
import scala.scalajs.js.typedarray.{Uint8Array, ArrayBuffer}

object NativeUtils {

  val jsString = js.Dynamic.global.String

  def ab2str(ab: ArrayBuffer): String = {
    // hax: we have to call a js method "apply" on the function property here
    // the apply from scala.dynamic is *not* the method we want
    jsString.fromCharCode.applyDynamic("apply")(null, new Uint8Array(ab)).asInstanceOf[String]
  }

  def str2ab(str: String): ArrayBuffer = {
    val buf = new ArrayBuffer(str.length)
    val bufView = new Uint8Array(buf)
    for {
      i <- str.indices
    } bufView(i) = str.charAt(i).toShort
    buf
  }

}
