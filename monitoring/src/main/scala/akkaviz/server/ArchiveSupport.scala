package akkaviz.server

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.Marshalling.WithFixedContentType
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Source}
import akkaviz.persistence.{PersistenceSources, ReceivedRecord}
import akkaviz.rest
import com.datastax.driver.core.utils.UUIDs
import org.reactivestreams.Publisher

import scala.concurrent.ExecutionContext.Implicits.global

trait ArchiveSupport {

  def archiveRouting: Route = get {
    pathPrefix("messages") {
      path("of" / Segment) {
        ref =>
          AkkaHttpHelpers.completeAsJson(receivedRecordsSource(PersistenceSources.of(ref)))
      } ~
        path("between" / Segment / Segment) {
          (ref, ref2) =>
            AkkaHttpHelpers.completeAsJson(receivedRecordsSource(PersistenceSources.between(ref, ref2)))
        }
    }
  }

  private[this] implicit val receivedRecordMarshaller: Marshaller[rest.Received, String] = Marshaller.strict {
    received =>
      WithFixedContentType(MediaTypes.`application/json`, () => upickle.default.write(received))
  }

  private[this] def receivedRecordsSource(publisher: Publisher[ReceivedRecord]): Source[rest.Received, _] = {
    Source.fromPublisher(publisher).via(receivedRecordToRestReceived)
  }

  private[this] def receivedRecordToRestReceived = Flow[ReceivedRecord].map {
    rr =>
      rest.Received(UUIDs.unixTimestamp(rr.id), rr.first, rr.second, rr.data)
  }

}
