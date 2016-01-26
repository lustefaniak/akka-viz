package akka.viz

import scala.pickling.Defaults._
import scala.pickling.json._
import scala.util.{Success, Try}

object MessageSerialization {

  def serialize(message: Any): String = {
    Try {
      message.pickle.value
    }.recoverWith {
      case t: Throwable =>
        Success(s"{'error':'Failed to serialize: ${t.getMessage}'}")
    }.get
  }

}
