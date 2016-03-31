package akkaviz.serialization.serializers

class OptionSerializerTest extends SerializerTest {

  test("It works for None") {
    OptionSerializer.canSerialize(None) shouldBe true
    val json: String = OptionSerializer.serialize(None, context)
    json shouldBe "null"
  }

  test("It works for Some") {
    OptionSerializer.canSerialize(Some(1)) shouldBe true
    val json: String = OptionSerializer.serialize(Some(1), context)
    json shouldBe "1"
  }

}
