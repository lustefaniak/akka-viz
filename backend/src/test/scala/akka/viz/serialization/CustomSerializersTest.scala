package akka.viz.serialization

import org.scalatest.{Matchers, FlatSpec}

class CustomSerializersTest extends FlatSpec with Matchers {

  "MessageSerialization" should "have default serializers" in {

    MessageSerialization.render("Test") shouldBe "\"Test\""
    MessageSerialization.render(1) shouldBe "1"
    MessageSerialization.render(1L) shouldBe "1"
    MessageSerialization.render(1.1) shouldBe "1.1"
    MessageSerialization.render(true) shouldBe "true"

  }


}
