package akkaviz.frontend

import akkaviz.protocol
import akkaviz.protocol.{ApiClientMessage, ApiServerMessage, IO}
import org.scalajs.dom.raw._
import rx.{Rx, Var}

import scala.concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetTimeoutHandle
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer, TypedArrayBufferOps}

object ApiConnection {

  trait Upstream {

    def isConnected: Boolean

    def connect(): Upstream

    def disconnect(): Upstream

    def send(msg: ApiClientMessage): Upstream

    def status: Rx[ConnectionStatus]

    def terminate(): Upstream

  }

  sealed trait ConnectionStatus

  case object Connected extends ConnectionStatus

  case object Connecting extends ConnectionStatus

  case object Disconnected extends ConnectionStatus

  case class Reconnecting(retry: Int, maxRetries: Int) extends ConnectionStatus

  case object GaveUp extends ConnectionStatus

  def apply(url: String, onWsOpen: Upstream => Unit, handler: protocol.ApiServerMessage => Unit, maxRetries: Int = 10): Upstream = {

    def createWebsocket: WebSocket = {
      val ws = new WebSocket(url)
      ws.binaryType = "arraybuffer"
      ws
    }

    new Upstream {
      private[this] var messagesToSend = js.Array[ArrayBuffer]()
      private[this] var connectedWebsocket: js.UndefOr[WebSocket] = js.undefined
      private[this] var connectingWebsocket: js.UndefOr[WebSocket] = js.undefined
      private[this] var disconnectedManually = false
      private[this] var retries: Int = 0
      private[this] var reconnectTimer: js.UndefOr[SetTimeoutHandle] = js.undefined

      private[this] def scheduleReconnect(): Unit = {
        if (reconnectTimer.isEmpty) {
          reconnectTimer = timers.setTimeout(2.seconds) {
            reconnectTimer = js.undefined
            openWebSocket
          }
        }
      }

      private[this] def handleAutoReconnect(): Unit = {
        if (!disconnectedManually) {
          if (retries < maxRetries) {
            scheduleReconnect()
          } else {
            status() = GaveUp
          }
        }
      }

      private[this] def connected(): Unit = {
        connectedWebsocket = connectingWebsocket
        connectingWebsocket = js.undefined
        retries = 0
        status() = Connected
        connectedWebsocket.foreach {
          ws =>
            messagesToSend.foreach(ws.send)
            messagesToSend = js.Array()
        }
      }

      private[this] def disconnected(error: Boolean): Unit = {
        connectedWebsocket = js.undefined
        connectingWebsocket = js.undefined
        if (disconnectedManually)
          status() = Disconnected
        handleAutoReconnect()
      }

      private[this] def openWebSocket(): Unit = {
        if (connectingWebsocket.isEmpty && connectedWebsocket.isEmpty) {
          val ws = createWebsocket
          connectingWebsocket = ws
          ws.onopen = (e: Event) => {
            connected()
          }
          ws.onerror = (e: ErrorEvent) => {
            disconnected(true)

          }
          ws.onclose = (e: CloseEvent) => {
            disconnected(false)
          }
          ws.onmessage = (messageEvent: MessageEvent) => {
            val bb = TypedArrayBuffer.wrap(messageEvent.data.asInstanceOf[ArrayBuffer])
            val message: ApiServerMessage = protocol.IO.readServer(bb)
            handler(message)
          }
          status() = if (retries == 0) Connecting else Reconnecting(retries, maxRetries)
          retries += 1
        }
      }

      /**
       * Try connecting to the WebSocket, retrying on failures
       *
       * @return Upstream
       */
      def connect(): Upstream = {
        retries = 0
        disconnectedManually = false
        openWebSocket()
        this
      }

      /**
       * Close connection and do not try to re-connect
       *
       * @return Upstream
       */
      def disconnect(): Upstream = {
        retries = 0
        disconnectedManually = true
        connectedWebsocket.foreach(_.close())
        this
      }

      /**
       * Terminate connection and try to reconnect
       *
       * @return Upstream
       */
      def terminate(): Upstream = {
        retries = 0
        disconnectedManually = false
        connectedWebsocket.foreach(_.close())
        this
      }

      /**
       * Send message to the server, if is unable to send queue the messages and send them when connected
       *
       * @param msg ApiClientMessage
       * @return
       */
      def send(msg: ApiClientMessage): Upstream = {
        val encoded = IO.write(msg)
        val ab = new TypedArrayBufferOps(encoded).arrayBuffer()
        connectedWebsocket.fold[Unit] {
          messagesToSend.push(ab)
        } {
          ws => ws.send(ab)
        }
        this
      }

      override def isConnected: Boolean = connectedWebsocket.isDefined

      val status: Var[ConnectionStatus] = Var(Disconnected)
    }
  }

}