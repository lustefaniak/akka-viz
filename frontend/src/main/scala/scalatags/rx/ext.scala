package scalatags.rx

import org.scalajs.dom._
import rx._

import scala.language.implicitConversions
import scala.scalajs.js

trait RxExt {

  implicit def richObs(o: Obs): RichObs = new RichObs(o)

}

private trait ReferenceHolder extends js.Object {
  var `scalatags.rx.refs`: js.UndefOr[js.Array[Any]] = js.native
}

class RichObs(val o: Obs) extends AnyVal {
  def attachTo(e: Node): Unit = {
    val holder = e.asInstanceOf[ReferenceHolder]
    if (holder.`scalatags.rx.refs`.isEmpty) {
      holder.`scalatags.rx.refs` = js.Array[Any]()
    }
    holder.`scalatags.rx.refs`.get.push(e)
  }
}
