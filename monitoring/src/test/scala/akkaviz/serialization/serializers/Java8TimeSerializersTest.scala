package akkaviz.serialization.serializers

import java.time.{LocalDate, LocalDateTime}

class Java8TimeSerializersTest extends SerializerTest {
  test("Is able to serialize LocalDateTime") {

    val dateTime = LocalDateTime.now

    Java8TimeSerializers.canSerialize("") shouldBe false
    Java8TimeSerializers.canSerialize(dateTime) shouldBe true

    val json: String = Java8TimeSerializers.serialize(dateTime, context)

    json should include(dateTime.toString)

  }

  test("Is able to serialize LocalDate") {

    val date = LocalDate.now

    Java8TimeSerializers.canSerialize("") shouldBe false
    Java8TimeSerializers.canSerialize(date) shouldBe true

    val json: String = Java8TimeSerializers.serialize(date, context)

    json should include(date.toString)

  }
}
