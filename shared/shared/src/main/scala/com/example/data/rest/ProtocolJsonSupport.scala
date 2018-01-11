package com.example.data.rest

import play.api.libs.json._

import com.example.data.websocket.DuplexProtocol._
import com.example.data.websocket.Protocol._
import scala.reflect.ClassTag

object ProtocolJsonSupportImpl {

  val classNameKey = "$type"
  val dataKey = "$data"


  import JsonSupport._

  implicit val monitorJoinedFormat = Json.format[MonitorJoined]
  implicit val monitorLeftFormat = Json.format[MonitorLeft]
  implicit val updateDuplicateFormat = Json.format[UpdateDuplicate]
  implicit val updateDuplicateHandFormat = Json.format[UpdateDuplicateHand]
  implicit val updateDuplicateTeamFormat = Json.format[UpdateDuplicateTeam]
  implicit val noDataFormat = Json.format[NoData]

  implicit val StartMonitorSummaryFormat = Json.format[StartMonitorSummary]
  implicit val StopMonitorSummaryFormat = Json.format[StopMonitorSummary]
  implicit val StartMonitorFormat = Json.format[StartMonitor]
  implicit val StopMonitorFormat = Json.format[StopMonitor]

}

abstract class SealedFormat[T]( implicit baseclass: ClassTag[T] ) extends Format[T] {
  import ProtocolJsonSupportImpl._

  val baseclassname = baseclass.runtimeClass.getName

  /**
   * @return None if className is unknown
   */
  def unmarshall( className: String, value: JsObject ): Option[JsResult[T]]
  def marshall( obj: T ): JsValue

  import scala.language.implicitConversions
  implicit def toT[ A <: T]( a: A ): T = a

  def reads(json: JsValue): JsResult[T] = {
    json match {
      case JsObject(map) =>
        map.get(classNameKey) match {
          case Some( JsString(className)) =>
            map.get(dataKey) match {
              case Some( JsNull ) =>
                JsSuccess(null.asInstanceOf[T])
              case Some( o: JsObject ) =>
                unmarshall(className,o) match {
                  case Some(res) => res
                  case None =>
                    JsError( JsonValidationError( Seq(s"Reading ${baseclassname}: Unknown class ${className}: ${Json.stringify(json)}") )  )
                }
              case Some(x) =>
                JsError( JsonValidationError( Seq(s"Reading ${baseclassname}: Expecting object or null for ${dataKey}: ${Json.stringify(x)}") )  )
              case None =>
                JsError( JsonValidationError( Seq(s"Reading ${baseclassname}: Missing key ${dataKey}: ${Json.stringify(json)}") )  )
            }
          case Some(x) =>
            JsError( JsonValidationError( Seq(s"Reading ${baseclassname}: Expecting string for ${classNameKey}: ${Json.stringify(x)}") )  )
          case None =>
            JsError( JsonValidationError( Seq(s"Reading ${baseclassname}: Missing key ${classNameKey}: ${Json.stringify(json)}") )  )
        }
      case _ =>
        JsError( JsonValidationError( Seq("Reading ${baseclassname}: Expecting a JSON object, got ${json.getClass.getSimpleName}: ${json}") )  )
    }
  }

  def writes(o: T): JsValue = {
    Json.obj( classNameKey -> JsString(o.getClass.getName), dataKey->marshall(o) )
  }
}


class ToServerMessageFormat extends SealedFormat[ToServerMessage] {
  import ProtocolJsonSupportImpl._
  /**
   * @return None if class name is unknown
   */
  def unmarshall( className: String, o: JsObject ): Option[JsResult[ToServerMessage]] = {
    if (className == classOf[StartMonitorSummary].getName) {
      Option(Json.fromJson[StartMonitorSummary](o).map{ o => o })
    } else if (className == classOf[StopMonitorSummary].getName) {
      Option(Json.fromJson[StopMonitorSummary](o).map{ o => o})
    } else if (className == classOf[StartMonitor].getName) {
      Option(Json.fromJson[StartMonitor](o).map{ o => o})
    } else if (className == classOf[StopMonitor].getName) {
      Option(Json.fromJson[StopMonitor](o).map{ o => o})
    } else if (className == classOf[UpdateDuplicate].getName) {
      Option(Json.fromJson[UpdateDuplicate](o).map{ o => o})
    } else if (className == classOf[UpdateDuplicateHand].getName) {
      Option(Json.fromJson[UpdateDuplicateHand](o).map{ o => o})
    } else if (className == classOf[UpdateDuplicateTeam].getName) {
      Option(Json.fromJson[UpdateDuplicateTeam](o).map{ o => o})
    } else if (className == classOf[NoData].getName) {
      Option(Json.fromJson[NoData](o).map{ o => o})
    } else {
      None
    }
  }

  def marshall( obj: ToServerMessage ): JsValue = {
    obj match {
      case x: StartMonitorSummary => Json.toJson(x)
      case x: StopMonitorSummary => Json.toJson(x)
      case x: StartMonitor => Json.toJson(x)
      case x: StopMonitor => Json.toJson(x)
      case x: UpdateDuplicate => Json.toJson(x)
      case x: UpdateDuplicateHand => Json.toJson(x)
      case x: UpdateDuplicateTeam => Json.toJson(x)
      case x: NoData => Json.toJson(x)
    }
  }
}

class ToBrowserMessageFormat extends SealedFormat[ToBrowserMessage] {
  import ProtocolJsonSupportImpl._
  /**
   * @return None if class name is unknown
   */
  def unmarshall( className: String, o: JsObject ): Option[JsResult[ToBrowserMessage]] = {
    if (className == classOf[MonitorJoined].getName) {
      Option(Json.fromJson[MonitorJoined](o).map{ o => o })
    } else if (className == classOf[MonitorLeft].getName) {
      Option(Json.fromJson[MonitorLeft](o).map{ o => o})
    } else if (className == classOf[UpdateDuplicate].getName) {
      Option(Json.fromJson[UpdateDuplicate](o).map{ o => o})
    } else if (className == classOf[UpdateDuplicateHand].getName) {
      Option(Json.fromJson[UpdateDuplicateHand](o).map{ o => o})
    } else if (className == classOf[UpdateDuplicateTeam].getName) {
      Option(Json.fromJson[UpdateDuplicateTeam](o).map{ o => o})
    } else if (className == classOf[NoData].getName) {
      Option(Json.fromJson[NoData](o).map{ o => o})
    } else {
      None
    }
  }

  def marshall( obj: ToBrowserMessage ): JsValue = {
    obj match {
      case x: MonitorJoined => Json.toJson[MonitorJoined](x)
      case x: MonitorLeft => Json.toJson[MonitorLeft](x)
      case x: UpdateDuplicate => Json.toJson[UpdateDuplicate](x)
      case x: UpdateDuplicateHand => Json.toJson[UpdateDuplicateHand](x)
      case x: UpdateDuplicateTeam => Json.toJson[UpdateDuplicateTeam](x)
      case x: NoData => Json.toJson[NoData](x)
    }
  }
}

trait ToBrowserProtocolJsonSupport {
  implicit val toBrowserMessageFormat = new ToBrowserMessageFormat    // Json.format[ToBrowserMessage]
}

trait ToServerProtocolJsonSupport {
  implicit val toServerMessageFormat = new ToServerMessageFormat  // Json.format[ToServerMessage]
}

object ToBrowserProtocolJsonSupport extends ToBrowserProtocolJsonSupport
object ToServerProtocolJsonSupport extends ToServerProtocolJsonSupport

