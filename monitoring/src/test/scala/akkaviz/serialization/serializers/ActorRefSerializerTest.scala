package akkaviz.serialization.serializers

import akka.actor.ActorSystem

class ActorRefSerializerTest extends SerializerTest {

  test("Is able to serialize ActorRef") {
    val system = ActorSystem()
    val ref = system.deadLetters

    ActorRefSerializer.canSerialize("") shouldBe false
    ActorRefSerializer.canSerialize(ref) shouldBe true

    val json: String = ActorRefSerializer.serialize(ref, context)

    json shouldBe """{"$type":"akka.actor.ActorRef","path":"akka://default/deadLetters"}"""

  }

}
