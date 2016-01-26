package akka.viz.frontend

import org.scalajs.dom._
import org.scalajs.dom.ext.LocalStorage
import rx.Var
import upickle.default._

trait Persistence {

  def persistedVar[T: Writer : Reader](initialValue: T, name: String): Var[T] = {
    val v = Var[T](LocalStorage(name).map(s => read[T](s)).getOrElse(initialValue))
    val storeObs = v.triggerLater {
      LocalStorage.update(name, upickle.default.write(v.now))
      console.log(s"persisted $name=${v.now}")
    }

    v
  }
}
