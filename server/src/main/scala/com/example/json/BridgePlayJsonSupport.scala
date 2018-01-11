package com.example.json

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Writes
import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import scala.language.implicitConversions
import com.example.data.rest.JsonSupport
import play.api.libs.json.Reads

trait BridgePlayJsonSupport extends JsonSupport {

  implicit def playJsonUnmarshallerConverter[T](reader: Reads[T]): FromEntityUnmarshaller[T] =
    unmarshaller(reader)

  implicit def unmarshaller[A: Reads]: FromEntityUnmarshaller[A] = PlayJsonSupport.unmarshaller

  implicit def playJsonMarshallerConverter[T](writer: Writes[T])(implicit printer: JsValue => String = Json.stringify): ToEntityMarshaller[T] =
    playJsonMarshaller[T](writer, printer)

  implicit def playJsonMarshaller[T](implicit writer: Writes[T], printer: JsValue => String = Json.stringify): ToEntityMarshaller[T] =
    PlayJsonSupport.marshaller // compose writer.writes


}
