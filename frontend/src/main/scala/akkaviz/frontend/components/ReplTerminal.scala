package akkaviz.frontend.components

import akkaviz.frontend.terminal._
import akkaviz.frontend.{DOMGlobalScope, FrontendUtil}
import org.scalajs.dom.raw.{CloseEvent, Element, ErrorEvent, WebSocket}
import org.scalajs.dom.{html, Event, MessageEvent}

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import scalatags.JsDom.all._

class ReplTerminal extends Component {

  private def options = js.Dynamic.literal(
    cols = 120,
    rows = 25
  ).asInstanceOf[TerminalOptions]

  private var ws: js.UndefOr[WebSocket] = js.undefined

  private lazy val terminal = new Terminal(options)

  private def setupWebsocket(): Unit = {

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

  private def setupReplTerminal(element: Element): Unit = {
    setupWebsocket()
    terminal.open(element)
  }

  override def render: html.Element = {
    val d = div().render
    setupReplTerminal(d)
    d
  }
}
