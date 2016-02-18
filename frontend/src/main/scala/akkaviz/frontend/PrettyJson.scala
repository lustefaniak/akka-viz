package akkaviz.frontend

import scala.scalajs.js
import scala.scalajs.js.JSON

trait PrettyJson {

  def prettyPrintJson(json: String): String = {
    val parsed = JSON.parse(json)
    JSON.stringify(parsed, null.asInstanceOf[js.Array[js.Any]], 2)
  }

}
