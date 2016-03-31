package akkaviz.server

import java.util.UUID

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source
import akkaviz.persistence.ReceivedRecord
import org.scalatest.{Matchers, FunSuite}

class ArchiveSupportTest extends FunSuite with ScalatestRouteTest with Matchers {

  val uuid = UUID.randomUUID()

  val ofGet = Get("/messages/of/ref")
  val betweenGet = Get("/messages/between/ref/ref2")

  test("Allows to get archive of Actor") {
    val arch = new ArchiveSupport {
      override def isArchiveEnabled: Boolean = true

      override def receivedBetween(ref: String, ref2: String): Source[ReceivedRecord, _] = fail("Should not be called")

      override def receivedOf(ref: String): Source[ReceivedRecord, _] =
        Source.single(ReceivedRecord(uuid, 100, ref, akkaviz.persistence.To, "second", "{}"))
    }

    ofGet ~> arch.archiveRouting ~> check {
      responseAs[String] shouldBe
        """[{"timestamp":"100","direction":">","from":"ref","to":"second","payload":"{}"}]"""
    }
  }

  test("Allows to get archive between two Actors") {
    val arch = new ArchiveSupport {
      override def isArchiveEnabled: Boolean = true

      override def receivedBetween(ref: String, ref2: String): Source[ReceivedRecord, _] =
        Source.single(ReceivedRecord(uuid, 200, ref, akkaviz.persistence.To, ref2, "{}"))

      override def receivedOf(ref: String): Source[ReceivedRecord, _] = fail("Should not be called")
    }

    betweenGet ~> arch.archiveRouting ~> check {
      responseAs[String] shouldBe
        """[{"timestamp":"200","direction":">","from":"ref","to":"ref2","payload":"{}"}]"""
    }
  }

  test("Archive could be disabled") {
    val arch = new ArchiveSupport {
      override def isArchiveEnabled: Boolean = false

      override def receivedBetween(ref: String, ref2: String): Source[ReceivedRecord, _] = fail("Should not be called")

      override def receivedOf(ref: String): Source[ReceivedRecord, _] = fail("Should not be called")
    }

    ofGet ~> arch.archiveRouting ~> check {
      handled shouldBe false
    }

    betweenGet ~> arch.archiveRouting ~> check {
      handled shouldBe false
    }
  }

}
