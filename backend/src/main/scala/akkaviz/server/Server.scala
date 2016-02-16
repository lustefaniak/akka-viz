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
  private lazy val system: ActorSystem = ActorSystem(Config.internalSystemName)
  private lazy val materializer = ActorMaterializer()(system)
  private lazy val service = new Webservice()(materializer, system)
  private lazy val binding = Http(system).bindAndHandle(service.route, Config.interface, Config.port)(materializer)

  def start() = {
    binding.onComplete {
      case Success(binding) ⇒
        val localAddress = binding.localAddress
        println(s"Server is listening on http://${localAddress.getHostName}:${localAddress.getPort}")
      case Failure(e) ⇒
        println(s"Binding failed with ${e.getMessage}")
        sys.exit(1)
    }(system.dispatcher)

    MessageSerialization.preload()

    Await.result(binding, Duration.Inf)
  }

}
