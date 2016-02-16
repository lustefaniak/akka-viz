package akkaviz.frontend

import akkaviz.protocol.ApiServerMessage

object ApiMessages {
  def read(json: String): ApiServerMessage = {
    import upickle.default._
    upickle.default.read[ApiServerMessage](json)
  }
}
