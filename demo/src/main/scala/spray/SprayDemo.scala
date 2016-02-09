package spray

import akka.actor._
import spray.routing.SimpleRoutingApp

object SprayDemo extends SimpleRoutingApp {

  def run(implicit system: ActorSystem) {

    startServer(interface = "localhost", port = 8080) {
      path("hello") {
        get {
          complete {
            <h1>Say hello to spray</h1>
          }
        }
      }
    }
  }
}
