package akkaviz.serialization.serializers

class ThrowableSerializerTest extends SerializerTest {

  test("It ignores other types") {
    ThrowableSerializer.canSerialize("test") shouldBe false
  }

  test("It serializes Throwables") {
    val ex = new Exception("message")
    ThrowableSerializer.canSerialize(ex) shouldBe true
    val json: String = ThrowableSerializer.serialize(ex, context)
    json should include(ex.getMessage)
  }

}
