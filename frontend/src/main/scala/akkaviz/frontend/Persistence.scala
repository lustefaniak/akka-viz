package akkaviz.frontend

import org.scalajs.dom._
import org.scalajs.dom.ext.LocalStorage
import rx.Var
import upickle.default._

import scala.util.Try

trait Persistence {

  def persistedVar[T: Writer: Reader](initialValue: T, name: String): Var[T] = {
    val init: T =
      LocalStorage(name).flatMap { stored =>
        val unpickled: Try[T] = Try(read[T](stored))
        unpickled.failed.foreach(thr => console.log(s"failed to unpickle $name, $thr"))

        unpickled.toOption
      }.getOrElse(initialValue)

    val v = Var[T](init)

    val storeObs = v.triggerLater {
      LocalStorage.update(name, upickle.default.write(v.now))
      console.log(s"persisted $name=${v.now}")
    }

    v
  }
}
