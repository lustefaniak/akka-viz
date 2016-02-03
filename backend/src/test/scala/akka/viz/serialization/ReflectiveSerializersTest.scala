package akka.viz.serialization

import org.scalatest.{FlatSpec, Matchers}
import upickle.Js
import upickle.json.FastRenderer

class ReflectiveSerializersTest extends FlatSpec with Matchers {

  "ReflectiveSerializer" should "use default serializers recursively" in {

    val keys = MessageSerialization.serialize(new {
      val X = 123
      val y = "Test"
    }).value.asInstanceOf[Seq[(String, Js.Value)]]

    keys.find(_._1 == "X") shouldBe Some("X" -> Js.Num(123))
    keys.find(_._1 == "y") shouldBe Some("y" -> Js.Str("Test"))

  }

  it should "work with nested objects" in {
    val serialized = MessageSerialization.serialize(new {
      val obj = new {
        val X = 1
      }
    })

    val json = FastRenderer.render(serialized)
    json should include("\"X\":1")

  }


}
