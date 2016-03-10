package akkaviz.server

import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.stream.ActorMaterializer
import akkaviz.config.Config
import akkaviz.serialization.MessageSerialization

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object Server {
  private[this] lazy val system: ActorSystem = ActorSystem(Config.internalSystemName)
  private[this] lazy val materializer = ActorMaterializer()(system)
  private[this] lazy val service = new Webservice()(materializer, system)
  private[this] lazy val binding = Http(system).bindAndHandle(service.route, Config.interface, Config.port)(materializer)

  def start(): Unit = {
    binding.onComplete {
      case Success(binding) =>
        val localAddress = binding.localAddress
        println(s"Server is listening on http://${localAddress.getHostName}:${localAddress.getPort}")
      case Failure(e) =>
        println(s"Binding failed with ${e.getMessage}")
        sys.exit(1)
    }(system.dispatcher)

    MessageSerialization.preload()

    Await.result(binding, Duration.Inf)
  }

}
