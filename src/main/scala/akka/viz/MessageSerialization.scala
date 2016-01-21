package akka.viz

import spray.json.JsObject
import upickle.Js
import upickle.default.writeJs

object MessageSerialization {

  def serialize(message: Any): Js.Value = {
    message match {
      case s: String =>
        writeJs(s)
      case n: Long =>
        writeJs(n)
      case n: Int =>
        writeJs(n)

      case msg => {
        Js.Obj(
          "$tpe" -> Js.Str(msg.getClass.getCanonicalName)
        )
      }

    }
  }

}
