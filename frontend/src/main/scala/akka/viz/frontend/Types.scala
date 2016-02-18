package akka.viz.frontend

import scala.scalajs.js

object Types {
  type FastStringSet = js.Dictionary[Unit]

  def emptyStringSet(): FastStringSet = js.Dictionary()

  implicit class IterableToFastStringSet(it: Iterable[String]) {
    def toFastStringSet: FastStringSet = it.foldLeft(emptyStringSet) {
      case (d, it) => d.update(it, ()); d
    }
  }

}
