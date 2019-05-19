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
  implicit val updateChicagoFormat = Json.format[UpdateChicago]
  implicit val updateChicagoRoundFormat = Json.format[UpdateChicagoRound]
  implicit val updateChicagoHandFormat = Json.format[UpdateChicagoHand]
  implicit val updateRubberFormat = Json.format[UpdateRubber]
  implicit val updateRubberHandFormat = Json.format[UpdateRubberHand]

  implicit val StartMonitorSummaryFormat = Json.format[StartMonitorSummary]
  implicit val StopMonitorSummaryFormat = Json.format[StopMonitorSummary]
  implicit val StartMonitorDuplicateFormat = Json.format[StartMonitorDuplicate]
  implicit val StopMonitorDuplicateFormat = Json.format[StopMonitorDuplicate]
  implicit val StartMonitorChicagoFormat = Json.format[StartMonitorChicago]
  implicit val StopMonitorChicagoFormat = Json.format[StopMonitorChicago]
  implicit val StartMonitorRubberFormat = Json.format[StartMonitorRubber]
  implicit val StopMonitorRubberFormat = Json.format[StopMonitorRubber]

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
    } else if (className == classOf[StartMonitorDuplicate].getName) {
      Option(Json.fromJson[StartMonitorDuplicate](o).map{ o => o})
    } else if (className == classOf[StopMonitorDuplicate].getName) {
      Option(Json.fromJson[StopMonitorDuplicate](o).map{ o => o})
    } else if (className == classOf[StartMonitorChicago].getName) {
      Option(Json.fromJson[StartMonitorChicago](o).map{ o => o})
    } else if (className == classOf[StopMonitorChicago].getName) {
      Option(Json.fromJson[StopMonitorChicago](o).map{ o => o})
    } else if (className == classOf[StartMonitorRubber].getName) {
      Option(Json.fromJson[StartMonitorRubber](o).map{ o => o})
    } else if (className == classOf[StopMonitorRubber].getName) {
      Option(Json.fromJson[StopMonitorRubber](o).map{ o => o})
    } else if (className == classOf[UpdateDuplicate].getName) {
      Option(Json.fromJson[UpdateDuplicate](o).map{ o => o})
    } else if (className == classOf[UpdateDuplicateHand].getName) {
      Option(Json.fromJson[UpdateDuplicateHand](o).map{ o => o})
    } else if (className == classOf[UpdateDuplicateTeam].getName) {
      Option(Json.fromJson[UpdateDuplicateTeam](o).map{ o => o})
    } else if (className == classOf[NoData].getName) {
      Option(Json.fromJson[NoData](o).map{ o => o})
    } else if (className == classOf[UpdateChicago].getName) {
      Option(Json.fromJson[UpdateChicago](o).map{ o => o})
    } else if (className == classOf[UpdateChicagoRound].getName) {
      Option(Json.fromJson[UpdateChicagoRound](o).map{ o => o})
    } else if (className == classOf[UpdateChicagoHand].getName) {
      Option(Json.fromJson[UpdateChicagoHand](o).map{ o => o})
    } else if (className == classOf[UpdateRubber].getName) {
      Option(Json.fromJson[UpdateRubber](o).map{ o => o})
    } else if (className == classOf[UpdateRubberHand].getName) {
      Option(Json.fromJson[UpdateRubberHand](o).map{ o => o})
    } else {
      None
    }
  }

  def marshall( obj: ToServerMessage ): JsValue = {
    obj match {
      case x: StartMonitorSummary => Json.toJson(x)
      case x: StopMonitorSummary => Json.toJson(x)
      case x: StartMonitorDuplicate => Json.toJson(x)
      case x: StopMonitorDuplicate => Json.toJson(x)
      case x: StartMonitorChicago => Json.toJson(x)
      case x: StopMonitorChicago => Json.toJson(x)
      case x: StartMonitorRubber => Json.toJson(x)
      case x: StopMonitorRubber => Json.toJson(x)
      case x: UpdateDuplicate => Json.toJson(x)
      case x: UpdateDuplicateHand => Json.toJson(x)
      case x: UpdateDuplicateTeam => Json.toJson(x)
      case x: NoData => Json.toJson(x)
      case x: UpdateChicago => Json.toJson(x)
      case x: UpdateChicagoRound => Json.toJson(x)
      case x: UpdateChicagoHand => Json.toJson(x)
      case x: UpdateRubber => Json.toJson(x)
      case x: UpdateRubberHand => Json.toJson(x)
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
    } else if (className == classOf[UpdateChicago].getName) {
      Option(Json.fromJson[UpdateChicago](o).map{ o => o})
    } else if (className == classOf[UpdateChicagoRound].getName) {
      Option(Json.fromJson[UpdateChicagoRound](o).map{ o => o})
    } else if (className == classOf[UpdateChicagoHand].getName) {
      Option(Json.fromJson[UpdateChicagoHand](o).map{ o => o})
    } else if (className == classOf[UpdateRubber].getName) {
      Option(Json.fromJson[UpdateRubber](o).map{ o => o})
    } else if (className == classOf[UpdateRubberHand].getName) {
      Option(Json.fromJson[UpdateRubberHand](o).map{ o => o})
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
      case x: UpdateChicago => Json.toJson[UpdateChicago](x)
      case x: UpdateChicagoRound => Json.toJson(x)
      case x: UpdateChicagoHand => Json.toJson(x)
      case x: UpdateRubber => Json.toJson(x)
      case x: UpdateRubberHand => Json.toJson(x)
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

