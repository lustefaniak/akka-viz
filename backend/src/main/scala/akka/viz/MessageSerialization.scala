package akka.viz

import upickle.Js

import scala.pickling.Defaults._
import scala.pickling.json._

object MessageSerialization {

  def serialize(message: Any):Js.Value = {
    Js.Str(message.pickle.value)
  }

}
