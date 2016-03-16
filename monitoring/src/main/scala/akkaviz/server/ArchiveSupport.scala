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

  def archiveRouting: Route = get {
    pathPrefix("messages") {
      if (Config.enableArchive) {
        path("of" / Segment) {
          ref =>
            AkkaHttpHelpers.completeAsJson(PersistenceSources.of(ref).via(receivedRecordToRestReceived))
        } ~
          path("between" / Segment / Segment) {
            (ref, ref2) =>
              AkkaHttpHelpers.completeAsJson(PersistenceSources.between(ref, ref2).via(receivedRecordToRestReceived))
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
      rest.Received(UUIDs.unixTimestamp(rr.id), rr.first, rr.second, rr.data)
  }

}
