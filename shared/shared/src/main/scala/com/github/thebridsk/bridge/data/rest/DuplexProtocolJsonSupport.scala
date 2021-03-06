package com.github.thebridsk.bridge.data.rest

import play.api.libs.json._

import com.github.thebridsk.bridge.data.websocket.DuplexProtocol._

trait ToBrowserDuplexProtocolJsonSupport {
  import ToBrowserProtocolJsonSupport.toBrowserMessageFormat

  implicit val responseFormat: OFormat[Response] = Json.format[Response]
  implicit val unsolicitedFormat: OFormat[Unsolicited] =
    Json.format[Unsolicited]

}

trait ToServerDuplexProtocolJsonSupport {
  import ToServerProtocolJsonSupport._

  implicit val sendFormat: OFormat[Send] = Json.format[Send]
  implicit val requestFormat: OFormat[Request] = Json.format[Request]

  implicit val errorResponseFormat: OFormat[ErrorResponse] =
    Json.format[ErrorResponse]
  implicit val logEntryV2Format: OFormat[LogEntryV2] = Json.format[LogEntryV2]
  implicit val logEntrySFormat: OFormat[LogEntryS] = Json.format[LogEntryS]
  implicit val completeFormat: OFormat[Complete] = Json.format[Complete]
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

      case x: Complete => Json.toJson[Complete](x)
      case x: Fail     => Json.toJson[Complete](Complete("Internal server error"))
    }
  }
}

trait DuplexProtocolJsonSupport {
  implicit val duplexMessageFormat: DuplexMessageFormat =
    new DuplexMessageFormat
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
