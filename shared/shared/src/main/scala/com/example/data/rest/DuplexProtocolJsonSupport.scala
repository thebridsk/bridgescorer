package com.example.data.rest

import play.api.libs.json._

import com.example.data.websocket.DuplexProtocol._
import com.example.data.websocket.Protocol._
import scala.reflect.ClassTag

trait ToBrowserDuplexProtocolJsonSupport {
  import ToBrowserProtocolJsonSupport.toBrowserMessageFormat

  implicit val responseFormat = Json.format[Response]
  implicit val unsolicitedFormat = Json.format[Unsolicited]

}

trait ToServerDuplexProtocolJsonSupport {
  import ToServerProtocolJsonSupport._

  implicit val sendFormat = Json.format[Send]
  implicit val requestFormat = Json.format[Request]

  implicit val errorResponseFormat = Json.format[ErrorResponse]
  implicit val logEntryV2Format = Json.format[LogEntryV2]
  implicit val logEntrySFormat = Json.format[LogEntryS]
}

trait DuplexProtocolJsonSupportImpl
    extends ToBrowserDuplexProtocolJsonSupport
    with ToServerDuplexProtocolJsonSupport
object DuplexProtocolJsonSupportImpl extends DuplexProtocolJsonSupportImpl

class DuplexMessageFormat extends SealedFormat[DuplexMessage] {
  import DuplexProtocolJsonSupportImpl._

  /**
    * @return None if class name is unknown
    */
  def unmarshall(
      className: String,
      o: JsObject
  ): Option[JsResult[DuplexMessage]] = {
    if (className == classOf[Response].getName) {
      Option(Json.fromJson[Response](o).map { o =>
        o
      })
    } else if (className == classOf[Unsolicited].getName) {
      Option(Json.fromJson[Unsolicited](o).map { o =>
        o
      })
    } else if (className == classOf[Send].getName) {
      Option(Json.fromJson[Send](o).map { o =>
        o
      })
    } else if (className == classOf[Request].getName) {
      Option(Json.fromJson[Request](o).map { o =>
        o
      })
    } else if (className == classOf[ErrorResponse].getName) {
      Option(Json.fromJson[ErrorResponse](o).map { o =>
        o
      })
    } else if (className == classOf[LogEntryV2].getName) {
      Option(Json.fromJson[LogEntryV2](o).map { o =>
        o
      })
    } else if (className == classOf[LogEntryS].getName) {
      Option(Json.fromJson[LogEntryS](o).map { o =>
        o
      })
    } else {
      None
    }
  }

  def marshall(obj: DuplexMessage): JsValue = {
    obj match {
      case x: Response      => Json.toJson[Response](x)
      case x: Unsolicited   => Json.toJson[Unsolicited](x)
      case x: Send          => Json.toJson[Send](x)
      case x: Request       => Json.toJson[Request](x)
      case x: ErrorResponse => Json.toJson[ErrorResponse](x)
      case x: LogEntryV2    => Json.toJson[LogEntryV2](x)
      case x: LogEntryS     => Json.toJson[LogEntryS](x)
    }
  }
}

trait DuplexProtocolJsonSupport {
  implicit val duplexMessageFormat = new DuplexMessageFormat
}
object DuplexProtocolJsonSupport extends DuplexProtocolJsonSupport
//object Test {
//
//  import DuplexProtocolJsonSupport._
//
//  def read( json: JsValue ) = {
//    Json.fromJson[ToServerMessage](json)
//  }
//
//  def write( obj: UpdateDuplicate ) = {
//    Json.toJson(obj)
//  }
//}
