package akkaviz.frontend

import java.nio.ByteBuffer

import akkaviz.frontend.terminal._
import org.scalajs.dom.raw.{ErrorEvent, CloseEvent, Element, WebSocket}
import org.scalajs.dom.{Event, MessageEvent, console, window}

import scala.scalajs.js
import scala.scalajs.js.typedarray.{Uint8Array, ArrayBuffer}

trait ReplTerminal {

  private def options = js.Dynamic.literal(
    cols = 120,
    rows = 25
  ).asInstanceOf[TerminalOptions]

  private var ws: js.UndefOr[WebSocket] = js.undefined

  lazy val terminal = new Terminal(options)

  def setupWebsocket(): Unit = {

    def writeToTerminal(s: String): Unit = {
      terminal.write(s)
    }

    def decodeArrayBuffer(buffer: ArrayBuffer): String = {
      DOMGlobalScope.ab2str(buffer)
    }

    def encodeArrayBuffer(str: String): ArrayBuffer = {
      DOMGlobalScope.str2ab(str)
    }

    def disconnected(): Unit = {
      terminal.off("data")
      ws = js.undefined
    }

    val _ws = new WebSocket(FrontendUtil.webSocketUrl("repl"))
    _ws.binaryType = "arraybuffer"
    _ws.onopen = (event: Event) => {

      terminal.onData((caller: Any, d: String) => {
        _ws.send(encodeArrayBuffer(d))
      })

      _ws.onmessage = (msg: MessageEvent) => {
        msg.data match {
          case a: ArrayBuffer => writeToTerminal(decodeArrayBuffer(a))
          case s: String      => writeToTerminal(s)
          case other          => writeToTerminal(other.toString)
        }
      }

      _ws.onclose = (close: CloseEvent) => {
        disconnected()
      }

      _ws.onerror = (close: ErrorEvent) => {
        disconnected()
      }

    }

    ws = _ws
  }

  def setupReplTerminal(element: Element): Unit = {
    setupWebsocket()
    terminal.open(element)
  }
}
