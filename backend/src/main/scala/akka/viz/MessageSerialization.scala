package akka.viz

import scala.pickling.Defaults._
import scala.pickling.json._

object MessageSerialization {

  def serialize(message: Any): String = {
    message.pickle.value
  }

}
