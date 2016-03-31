package akkaviz.server

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

trait FrontendResourcesSupport {

  def frontendResourcesRouting: Route = {
    get {
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~
        path("frontend-launcher.js")(getFromResource("frontend-launcher.js")) ~
        path("frontend-fastopt.js")(getFromResource("frontend-fastopt.js")) ~
        path("frontend-fastopt.js.map")(getFromResource("frontend-fastopt.js.map")) ~
        path("frontend-jsdeps.js")(getFromResource("frontend-jsdeps.js")) ~
        getFromResourceDirectory("web") ~
        pathPrefix("webjars") {
          getFromResourceDirectory("META-INF/resources/webjars")
        }
    }
  }
}

object FrontendResourcesSupport extends FrontendResourcesSupport