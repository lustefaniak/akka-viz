package scalatags.rx

import java.util.concurrent.atomic.AtomicReference

import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.Comment
import rx._

import scala.collection.immutable
import scala.language.implicitConversions
import scalatags.JsDom.all._
import scalatags.jsdom
import scalatags.rx.ext._

trait RxNodeInstances {

  implicit class rxStringFrag(v: Rx[String])(implicit val ctx: Ctx.Owner) extends jsdom.Frag {
    def render: dom.Text = {
      val node = dom.document.createTextNode(v.now)
      v foreach { s => node.replaceData(0, node.length, s)} attachTo node
      node
    }
  }

  implicit class bindRxElement[T <: dom.Element](e: Rx[T])(implicit val ctx: Ctx.Owner) extends Modifier {
    def applyTo(t: Element) = {
      val element = new AtomicReference(e.now)
      t.appendChild(element.get())
      e.triggerLater {
        val current = e.now
        val previous = element getAndSet current
        t.replaceChild(current, previous)
      } attachTo t
    }
  }

  implicit class bindRxElements(e: Rx[immutable.Iterable[Element]])(implicit val ctx: Ctx.Owner) extends Modifier {
    def applyTo(t: Element) = {
      val nonEmpty = e.map { t => if (t.isEmpty) List(new Comment) else t}
      val fragments = new AtomicReference(nonEmpty.now)
      nonEmpty.now foreach t.appendChild
      nonEmpty triggerLater {
        val current = e.now
        val previous = fragments getAndSet current
        val i = t.childNodes.indexOf(previous.head)
        if (i < 0) throw new IllegalStateException("Children changed")
        0 to (previous.size - 1) foreach (_ => t.removeChild(t.childNodes.item(i)))
        if (t.childNodes.length > i) {
          val next = t.childNodes.item(i)
          current foreach (t.insertBefore(_, next))
        } else {
          current foreach t.appendChild
        }
      }
    }
  }

}
