package akkaviz.frontend

import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBufferView, Uint8Array, ArrayBuffer}

object NativeUtils {

  val encoder = new TextEncoder("utf-8")
  val decoder = new TextDecoder("utf-8")

  def ab2str(ab: ArrayBuffer): String = {
    decoder.decode(new Uint8Array(ab))
  }

  def str2ab(str: String): Uint8Array = {
    encoder.encode(str)
  }

}

@js.native
class TextEncoder(utfLabel: js.UndefOr[String]) extends js.Object {
  def encode(buffer: String): Uint8Array = js.native
}

@js.native
class TextDecoder(utfLabel: js.UndefOr[String]) extends js.Object {
  def decode(buffer: Uint8Array): String = js.native
}
