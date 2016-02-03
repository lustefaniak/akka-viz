package akka.viz.serialization

import org.scalatest.{Matchers, FlatSpec}

class CustomSerializersTest extends FlatSpec with Matchers {

  "MessageSerialization" should "have default serializers" in {

    MessageSerialization.serializeToString("Test") shouldBe "\"Test\""
    MessageSerialization.serializeToString(1) shouldBe "1"
    MessageSerialization.serializeToString(1L) shouldBe "1"
    MessageSerialization.serializeToString(1.1) shouldBe "1.1"
    MessageSerialization.serializeToString(true) shouldBe "true"

  }


}
