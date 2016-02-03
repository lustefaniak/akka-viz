package akka.viz.serialization

import akka.viz.serialization.test.JavaTest
import org.scalatest.{Matchers, FlatSpec}


class ReflectiveFieldsTest extends FlatSpec with Matchers {

  class MyActor[T] {

    var str = "Wroclaw"

    var city: Option[String] = None

    private val y: Option[T] = None

    def gggg = y
  }

  "ClassInspection.of" should "list all fields in MyActor" in {

    val actor = new MyActor[String]()
    ClassInspector.of(actor.getClass).fields.map {
      _.name
    } should contain theSameElementsAs Seq("city", "y", "str")
  }

  it should "work with anon class" in {
    ClassInspector.of((new AnyRef {
      val x = 1
    }).getClass).fields.map {
      _.name
    } should contain theSameElementsAs Seq("x")
  }

  it should "work with Java class" in {

    ClassInspector.of((new JavaTest).getClass).fields.map {
      _.name
    } should contain theSameElementsAs Seq("cx", "X", "x")

  }

  "Inspector" should "return values of all fields" in {
    val actor = new MyActor[String]()
    val inspector = ClassInspector.of(actor.getClass.asInstanceOf[Class[MyActor[String]]])

    inspector.inspect(actor) should contain theSameElementsAs Map(
      "city" -> None,
      "str" -> "Wroclaw",
      "y" -> None
    )
  }

  "Inspector" should "return values of single field" in {
    val actor = new MyActor[String]()
    val inspector = ClassInspector.of(actor.getClass.asInstanceOf[Class[MyActor[String]]])

    inspector.inspect(actor, Set("str")).get("str") shouldBe Some("Wroclaw")
  }


}
