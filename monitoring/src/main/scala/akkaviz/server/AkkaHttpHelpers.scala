package akkaviz.server

import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.{Directives, StandardRoute}
import akka.stream.scaladsl.{Flow, Source}

import scala.concurrent.ExecutionContext

trait AkkaHttpHelpers {

  def asJsonArray[T](implicit m: Marshaller[T, String], ec: ExecutionContext): Flow[T, HttpEntity.ChunkStreamPart, _] = {
    Flow.apply[T]
      .mapAsync[String](4)(t => Marshal(t).to[String])
      .scan[Option[ChunkStreamPart]](None) {
        case (None, s: String) => Some(ChunkStreamPart(s))
        case (_, s: String)    => Some(ChunkStreamPart(s",${s}"))
      }.mapConcat(_.toList)
      .prepend(Source.single(ChunkStreamPart("[")))
      .concat(Source.single(ChunkStreamPart("]")))
  }

  def completeAsJson[T](source: Source[T, _])(implicit m: Marshaller[T, String], ec: ExecutionContext): StandardRoute = {
    Directives.complete(HttpResponse(
      entity = HttpEntity.Chunked(MediaTypes.`application/json`, source.via(asJsonArray))
    ))
  }
}

object AkkaHttpHelpers extends AkkaHttpHelpers
