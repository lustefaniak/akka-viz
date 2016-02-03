package akka.viz.serialization

import org.scalatest.{Matchers, FlatSpec}

class CustomSerializersTest extends FlatSpec with Matchers {

  "MessageSerialization" should "have default serializers" in {

    MessageSerialization.serialize("Test") shouldBe "\"Test\""
    MessageSerialization.serialize(1) shouldBe "1"
    MessageSerialization.serialize(1L) shouldBe "1"
    MessageSerialization.serialize(1.1) shouldBe "1.1"
    MessageSerialization.serialize(true) shouldBe "true"

  }


}
