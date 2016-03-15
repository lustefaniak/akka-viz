package akkaviz.server

import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.{Directives, StandardRoute}
import akka.stream.scaladsl.Source
import org.reactivestreams.Publisher

import scala.concurrent.ExecutionContext

trait AkkaHttpHelpers {
  def completeAsJson[T](publisher: Publisher[T])(implicit m: Marshaller[T, String], ec: ExecutionContext): StandardRoute = {

    def asJsonArray(source: Source[T, _]): Source[ChunkStreamPart, _] = {
      val elements: Source[ChunkStreamPart, _] = source.mapAsync[String](4)(t => Marshal(t).to[String])
        .scan[Option[ChunkStreamPart]](None) {
          case (None, s: String) => Some(ChunkStreamPart(s))
          case (_, s: String)    => Some(ChunkStreamPart(s", ${s}"))
        }.mapConcat(_.toList)

      Source.single(ChunkStreamPart("[")) ++ elements ++ Source.single(ChunkStreamPart("]"))
    }

    val source = asJsonArray(Source.fromPublisher(publisher))
    Directives.complete(HttpResponse(
      entity = HttpEntity.Chunked(MediaTypes.`application/json`, source)
    ))
  }
}

object AkkaHttpHelpers extends AkkaHttpHelpers
