package akkaviz.server

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FunSuite, Matchers}

class FrontendResourcesSupportTest extends FunSuite with ScalatestRouteTest with Matchers {

  val route = FrontendResourcesSupport.frontendResourcesRouting

  test("It is possible to GET index.html") {
    Get() ~> route ~> check {
      handled shouldBe true
    }
  }

  test("It is possible to GET style.css") {
    Get("/css/style.css") ~> route ~> check {
      handled shouldBe true
    }
  }

  test("It is possible to GET frontend-launcher.js") {
    Get("/frontend-launcher.js") ~> route ~> check {
      handled shouldBe true
    }
  }

  test("It is possible to GET frontend-fastopt.js") {
    Get("/frontend-fastopt.js") ~> route ~> check {
      handled shouldBe true
    }
  }

  test("It is possible to GET frontend-fastopt.js.map") {
    Get("/frontend-fastopt.js.map") ~> route ~> check {
      handled shouldBe true
    }
  }

  test("It is possible to GET frontend-jsdeps.js") {
    Get("/frontend-jsdeps.js") ~> route ~> check {
      handled shouldBe true
    }
  }

  test("It is possible to GET something from WebJars") {
    Get("/webjars/vis/4.15.0/dist/vis.min.css") ~> route ~> check {
      handled shouldBe true
    }
  }

}
