package akkaviz.server

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ammonite.repl.Bind

trait ReplSupport extends WebSocketRepl {

  def replRouting: Route = {
    path("repl") {
      replWebSocket
    }
  }

  protected override def replArgs: Seq[Bind[_]] = Nil

  protected override def replPredef: String =
    """
      |import Predef.{println => _}
      |import pprint.{pprintln => println}
      |import akkaviz.events.ActorSystems.systems
      |import scala.concurrent.duration._
      |import akka.actor._
      |import akka.pattern._
    """.stripMargin

}
