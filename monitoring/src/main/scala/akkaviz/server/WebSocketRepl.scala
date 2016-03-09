package akkaviz.server

import java.io.{InputStream, OutputStream, PrintStream}
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import akka.Done
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl._
import akka.util.ByteString
import ammonite.ops.Path
import ammonite.repl._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

trait WebSocketRepl {

  def replPredef: String

  def replArgs: Seq[Bind[_]]

  def replThreadName: String = {
    s"Ammonite-REPL-${replCounter.incrementAndGet()}"
  }

  private[this] var replCounter = new AtomicInteger()

  def defaultReplPredef =
    """
      |import Predef.{println => _}
      |import pprint.{pprintln => println}
    """.stripMargin

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  def replWebSocket: Route = {

    def runRepl(replServerClassLoader: ClassLoader, in: InputStream, out: OutputStream) = {
      val homePath = Path(Path.makeTmp)
      val sshOut = new SshOutputStream(out)

      def withConsoleRedirection(fun: => Any): Unit = {
        Console.withIn(in) {
          Console.withOut(sshOut) {
            Console.withErr(sshOut) {
              fun
            }
          }
        }
      }

      val replSessionEnv = Environment(replServerClassLoader, in, sshOut)
      val runnable = new Runnable {
        override def run(): Unit = {
          try {
            Environment.withEnvironment(replSessionEnv) {
              withConsoleRedirection {
                val predef = defaultReplPredef + "\n" + replPredef
                val repl = new Repl(in, sshOut, sshOut, Ref(Storage(homePath, None)), predef, replArgs)
                repl.run()
                repl
              }
            }
          } catch {
            case NonFatal(t) =>
              val sshClientOutput = new PrintStream(sshOut)
              sshClientOutput.println("What a terrible failure, the REPL just blow up!")
              t.printStackTrace(sshClientOutput)
              sshOut.flush()
              sshOut.close()
          } finally {
            Try(in.close())
            Try(sshOut.close())
          }
        }
      }

      new Thread(runnable, replThreadName)
    }

    val wsFlow: Flow[Message, Message, _] = {

      val in = Flow[Message].flatMapConcat {
        case b: BinaryMessage => b.dataStream
        case t: TextMessage   => t.textStream.map(str => ByteString(str.getBytes))
      }.toMat(StreamConverters.asInputStream(1.hour))(Keep.right)

      val out = StreamConverters.asOutputStream(1.hour)
        .groupedWithin(1000, 100.millis)
        .map(_.reduce(_ ++ _))
        .map[Message](bs => BinaryMessage.Strict(bs))

      Flow.fromSinkAndSourceMat(in, out)(Keep.both).mapMaterializedValue {
        case (in, out) => {
          val thread = runRepl(this.getClass.getClassLoader, in, out)
          thread.start()
          thread
        }
      }.keepAlive(30.seconds, () => BinaryMessage.Strict(ByteString()))
    }.watchTermination() {
      (t: Thread, f: Future[Done]) =>
        f.onComplete { _ => t.interrupt() }
    }

    Directives.handleWebSocketMessages(wsFlow)

  }

  private[this] class SshOutputStream(out: OutputStream) extends OutputStream {
    override def close() = {
      out.close()
    }

    override def flush() = {
      out.flush()
    }

    override def write(byte: Int): Unit = {
      if (byte.toChar == '\n') out.write('\r')
      out.write(byte)
    }

    override def write(bytes: Array[Byte]): Unit = for {
      i <- bytes.indices
    } write(bytes(i))

    override def write(bytes: Array[Byte], offset: Int, length: Int): Unit = {
      write(bytes.slice(offset, offset + length))
    }
  }

  /**
   * Container for staging environment important for Ammonite repl to run correctly.
   *
   * @param thread             a thread where execution takes place. Important for restoring contextClassLoader
   * @param contextClassLoader thread's context class loader. Ammonite repl uses that to load classes
   * @param systemIn
   * @param systemOut
   * @param systemErr
   */
  private case class Environment(
    thread: Thread,
    contextClassLoader: ClassLoader,
    systemIn: InputStream,
    systemOut: PrintStream,
    systemErr: PrintStream
  )

  private object Environment {
    def apply(classLoader: ClassLoader, in: InputStream, out: PrintStream): Environment =
      apply(Thread.currentThread(), classLoader, in, out, out)

    def apply(classLoader: ClassLoader, in: InputStream, out: OutputStream): Environment =
      apply(classLoader, in, new PrintStream(out))

    /**
     * Collects information about current environment
     */
    def collect() = Environment(
      Thread.currentThread(),
      Thread.currentThread().getContextClassLoader,
      System.in,
      System.out,
      System.err
    )

    /**
     * Runs your code with supplied environment installed.
     * After execution of supplied code block will restore original environment
     */
    def withEnvironment(env: Environment)(code: => Any): Any = {
      val oldEnv = collect()
      try {
        install(env)
        code
      } finally {
        install(oldEnv)
      }
    }

    /**
     * Resets execution environment from parameters saved to Environment container passed in
     *
     * @param env environment to reset to
     */
    def install(env: Environment): Unit = {
      env.thread.setContextClassLoader(env.contextClassLoader)
      System.setIn(env.systemIn)
      System.setOut(env.systemOut)
      System.setErr(env.systemErr)
    }
  }

}
