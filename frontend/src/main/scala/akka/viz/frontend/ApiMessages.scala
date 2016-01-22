package akka.viz.frontend

import akka.viz.protocol.ApiServerMessage

object ApiMessages {
  def read(json: String): ApiServerMessage = {
    import upickle.default._
    upickle.default.read[ApiServerMessage](json)
  }
}
