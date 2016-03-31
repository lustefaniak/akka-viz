package akkaviz.protocol

import org.scalatest.{Matchers, FunSuite}

class ProtocolIOTest extends FunSuite with Matchers {

  test("Roundtrip Server") {
    val msg = Spawned("ref")
    IO.readServer(IO.write(msg)) shouldBe msg

  }

  test("Roundtrip Client") {
    val msg = SetEnabled(true)
    IO.readClient(IO.write(msg)) shouldBe msg
  }

}
