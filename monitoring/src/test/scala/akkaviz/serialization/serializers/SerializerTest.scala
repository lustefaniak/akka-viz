package akkaviz.serialization.serializers

import akkaviz.serialization.SerializationContext
import org.scalatest.{Matchers, FunSuite}
import upickle.Js
import upickle.json.FastRenderer

trait SerializerTest extends FunSuite with Matchers {

  val context = new SerializationContext {
    override def depth(): Int = 0
  }

  implicit def jsValueAsJson(js: Js.Value): String = FastRenderer.render(js)

}
