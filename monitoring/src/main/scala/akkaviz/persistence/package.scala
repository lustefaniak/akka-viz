package akkaviz

import java.util.UUID

import io.getquill._

package object persistence {

  val From = "<"

  val To = ">"

  case class ReceivedRecord(id: UUID, first: String, direction: String, second: String, data: String)

  object Queries {

    val getAllFor = quote {
      (actorRef: String) =>
        query[ReceivedRecord].filter(_.first == actorRef)
    }

    val getBetween = quote {
      (actorRef: String, actorRef2: String, direction: String) =>
        getAllFor(actorRef).filter(_.second == actorRef2).filter(_.direction == direction)
    }

  }

}
