package akkaviz.server

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.Marshalling.WithFixedContentType
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Source}
import akkaviz.config.Config
import akkaviz.persistence.{PersistenceSources, ReceivedRecord}
import akkaviz.rest
import com.datastax.driver.core.utils.UUIDs
import org.reactivestreams.Publisher

import scala.concurrent.ExecutionContext.Implicits.global

trait ArchiveSupport {

  def isArchiveEnabled: Boolean

  def receivedOf(ref: String): Source[ReceivedRecord, _]

  def receivedBetween(ref: String, ref2: String): Source[ReceivedRecord, _]

  def archiveRouting: Route = get {
    pathPrefix("messages") {
      if (isArchiveEnabled) {
        path("of" / Segment) {
          ref =>
            AkkaHttpHelpers.completeAsJson(receivedOf(ref).via(receivedRecordToRestReceived))
        } ~
          path("between" / Segment / Segment) {
            (ref, ref2) =>
              AkkaHttpHelpers.completeAsJson(receivedBetween(ref, ref2).via(receivedRecordToRestReceived))
          }
      } else {
        reject
      }
    }
  }

  private[this] implicit val receivedRecordMarshaller: Marshaller[rest.Received, String] = Marshaller.strict {
    received =>
      WithFixedContentType(MediaTypes.`application/json`, () => upickle.default.write(received))
  }

  private[this] def receivedRecordToRestReceived = Flow[ReceivedRecord].map {
    rr =>
      rest.Received(rr.millis, rr.direction, rr.first, rr.second, rr.data)
  }

}
