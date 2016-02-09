package akka.viz.serialization

import java.util.Locale

import org.scalatest.{FlatSpec, Matchers}

class ReflectiveSerializersTest extends FlatSpec with Matchers {

  "ReflectiveSerializer" should "use default serializers recursively" in {

    val json = MessageSerialization.render(new {
      val X = 123
      val y = "Test"
    })

    json should include("\"X\":123")
    json should include("\"y\":\"Test\"")

  }

  it should "work with nested objects" in {
    val json = MessageSerialization.render(new {
      val obj = new {
        val X = 1
      }
    })

    json should include("\"X\":1")

  }

  it should "handle top level null" in {
    MessageSerialization.render(null) shouldBe "null"
  }

  it should "handle null inside class" in {
    val json = MessageSerialization.render(new {
      val X: Locale = null
    })

    json should include("\"X\":null")
  }

  it should "handle cycles and finite serialization depth" in {
    case class X(var x: X)

    val x = X(null)
    val y = X(x)
    x.x = y

    MessageSerialization.render(x).size should be > 0

  }

}
