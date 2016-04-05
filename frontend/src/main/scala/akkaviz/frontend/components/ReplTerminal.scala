package akkaviz.frontend.components

import akkaviz.frontend.terminal.{Terminal, TerminalOptions}
import akkaviz.frontend.{FrontendUtil, NativeUtils}
import org.scalajs.dom.raw.HTMLButtonElement
import org.scalajs.dom.{Event, MessageEvent, _}

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import scalatags.JsDom.all._

class ReplTerminal extends Tab {

  private[this] def options = js.Dynamic.literal(
    cols = 64,
    rows = 15
  ).asInstanceOf[TerminalOptions]

  private[this] var ws: js.UndefOr[WebSocket] = js.undefined

  private[this] var terminal: js.UndefOr[Terminal] = js.undefined
  private[this] val connectButton: HTMLButtonElement = button(
    tpe := "button", `class` := "btn btn-default", "Connect"
  ).render
  private[this] var isConnected = false

  private[this] def connected(): Unit = {
    isConnected = true
    connectButton.innerHTML = "Disconnect"
  }

  private[this] def disconnected(): Unit = {
    isConnected = false
    terminal.foreach {
      terminal =>
        terminal.off("data")
        terminal.write("\n\r\n\rDisconnected\n\r\n\r")
    }
    ws = js.undefined
    connectButton.innerHTML = "Connect"
  }

  private[this] def setupWebsocket(): Unit = {

    def writeToTerminal(s: String): Unit = {
      terminal.foreach(_.write(s))
    }

    def decodeArrayBuffer(buffer: ArrayBuffer): String = {
      NativeUtils.ab2str(buffer)
    }

    def encodeArrayBuffer(str: String): ArrayBuffer = {
      NativeUtils.str2ab(str).buffer
    }

    val _ws = new WebSocket(FrontendUtil.webSocketUrl("repl"))
    _ws.binaryType = "arraybuffer"
    _ws.onopen = (event: Event) => {

      connected()
      terminal.foreach {
        terminal =>
          terminal.write("\n\r\n\rPlease wait, initializing server side REPL\n\r\n\r\r\r\n\r")

          terminal.onData((caller: Any, d: String) => {
            _ws.send(encodeArrayBuffer(d))
          })
      }

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
    val term = new Terminal(options)
    term.open(element)
    terminal = term
    js.Dynamic.global.terminal = terminal
  }

  override def name: String = "REPL"

  override def tabId: String = "repl-tab"

  override def onCreate(): Unit = {
    connectButton.onclick = (e: MouseEvent) => {
      if (isConnected) {
        ws.foreach {
          ws =>
            ws.send(NativeUtils.str2ab("\u0004").buffer)
            ws.close()
        }
      } else {
        setupWebsocket()
      }
    }
    tabBody.appendChild(connectButton)
    setupReplTerminal(tabBody)
  }
}
