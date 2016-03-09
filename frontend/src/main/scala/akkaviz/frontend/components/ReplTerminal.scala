package akkaviz.frontend.components

import akkaviz.frontend.terminal.{Terminal, TerminalOptions}
import akkaviz.frontend.{FrontendUtil, NativeUtils}
import org.scalajs.dom.raw.HTMLButtonElement
import org.scalajs.dom.{Event, MessageEvent, _}

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import scalatags.JsDom.all._

class ReplTerminal extends Component {

  private[this] def options = js.Dynamic.literal(
    cols = 120,
    rows = 25
  ).asInstanceOf[TerminalOptions]

  private[this] var ws: js.UndefOr[WebSocket] = js.undefined

  private[this] lazy val terminal = new Terminal(options)
  private[this] var connectButton: js.UndefOr[HTMLButtonElement] = js.undefined
  private[this] var isConnected = false

  private[this] def connected(): Unit = {
    isConnected = true;
    connectButton.foreach {
      _.innerHTML = "Disconnect"
    }
  }

  private[this] def disconnected(): Unit = {
    isConnected = false
    terminal.off("data")
    terminal.write("\n\r\n\rDisconnected\n\r\n\r")
    ws = js.undefined
    connectButton.foreach {
      _.innerHTML = "Connect"
    }
  }

  private[this] def setupWebsocket(): Unit = {

    def writeToTerminal(s: String): Unit = {
      terminal.write(s)
    }

    def decodeArrayBuffer(buffer: ArrayBuffer): String = {
      NativeUtils.ab2str(buffer)
    }

    def encodeArrayBuffer(str: String): ArrayBuffer = {
      NativeUtils.str2ab(str)
    }

    val _ws = new WebSocket(FrontendUtil.webSocketUrl("repl"))
    _ws.binaryType = "arraybuffer"
    _ws.onopen = (event: Event) => {

      connected()

      terminal.write("\n\r\n\rPlease wait, initializing server side REPL\n\r\n\r\r\r\n\r")

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

  private[this] def setupReplTerminal(element: Element): Unit = {
    terminal.open(element)
  }

  override def attach(parent: Element): Unit = {
    val b = button(tpe := "button", `class` := "btn btn-default", "Connect").render
    b.onclick = (e: MouseEvent) => {
      if (isConnected) {
        ws.foreach {
          ws =>
            ws.send(NativeUtils.str2ab("\u0004"))
            ws.close()
        }
      } else {
        setupWebsocket()
      }
    }
    connectButton = b
    val d = div().render
    setupReplTerminal(d)
    parent.appendChild(b)
    parent.appendChild(d)
  }
}
