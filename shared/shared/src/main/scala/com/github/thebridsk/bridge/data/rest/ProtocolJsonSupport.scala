package com.github.thebridsk.bridge.data.rest

import play.api.libs.json._

import com.github.thebridsk.bridge.data.websocket.Protocol._
import scala.reflect.ClassTag

object ProtocolJsonSupportImpl {

  val classNameKey = "$type"
  val dataKey = "$data"

  import JsonSupport._

  implicit val monitorJoinedFormat: OFormat[MonitorJoined] =
    Json.format[MonitorJoined]
  implicit val monitorLeftFormat: OFormat[MonitorLeft] =
    Json.format[MonitorLeft]
  implicit val updateDuplicateFormat: OFormat[UpdateDuplicate] =
    Json.format[UpdateDuplicate]
  implicit val updateDuplicateHandFormat: OFormat[UpdateDuplicateHand] =
    Json.format[UpdateDuplicateHand]
  implicit val updateDuplicateTeamFormat: OFormat[UpdateDuplicateTeam] =
    Json.format[UpdateDuplicateTeam]
  implicit val updateDuplicatePictureFormat: OFormat[UpdateDuplicatePicture] =
    Json.format[UpdateDuplicatePicture]
  implicit val updateDuplicatePicturesFormat: OFormat[UpdateDuplicatePictures] =
    Json.format[UpdateDuplicatePictures]
  implicit val updateIndividualDuplicateFormat: OFormat[UpdateIndividualDuplicate] =
    Json.format[UpdateIndividualDuplicate]
  implicit val updateIndividualDuplicateHandFormat: OFormat[UpdateIndividualDuplicateHand] =
    Json.format[UpdateIndividualDuplicateHand]
  implicit val updateIndividualDuplicatePictureFormat: OFormat[UpdateIndividualDuplicatePicture] =
    Json.format[UpdateIndividualDuplicatePicture]
  implicit val updateIndividualDuplicatePicturesFormat: OFormat[UpdateIndividualDuplicatePictures] =
    Json.format[UpdateIndividualDuplicatePictures]
  implicit val noDataFormat: OFormat[NoData] = Json.format[NoData]
  implicit val updateChicagoFormat: OFormat[UpdateChicago] =
    Json.format[UpdateChicago]
  implicit val updateChicagoRoundFormat: OFormat[UpdateChicagoRound] =
    Json.format[UpdateChicagoRound]
  implicit val updateChicagoHandFormat: OFormat[UpdateChicagoHand] =
    Json.format[UpdateChicagoHand]
  implicit val updateRubberFormat: OFormat[UpdateRubber] =
    Json.format[UpdateRubber]
  implicit val updateRubberHandFormat: OFormat[UpdateRubberHand] =
    Json.format[UpdateRubberHand]

  implicit val StartMonitorSummaryFormat: OFormat[StartMonitorSummary] =
    Json.format[StartMonitorSummary]
  implicit val StopMonitorSummaryFormat: OFormat[StopMonitorSummary] =
    Json.format[StopMonitorSummary]
  implicit val StartMonitorDuplicateFormat: OFormat[StartMonitorDuplicate] =
    Json.format[StartMonitorDuplicate]
  implicit val StopMonitorDuplicateFormat: OFormat[StopMonitorDuplicate] =
    Json.format[StopMonitorDuplicate]
  implicit val StartMonitorIndividualDuplicateFormat: OFormat[StartMonitorIndividualDuplicate] =
    Json.format[StartMonitorIndividualDuplicate]
  implicit val StopMonitorIndividualDuplicateFormat: OFormat[StopMonitorIndividualDuplicate] =
    Json.format[StopMonitorIndividualDuplicate]
  implicit val StartMonitorChicagoFormat: OFormat[StartMonitorChicago] =
    Json.format[StartMonitorChicago]
  implicit val StopMonitorChicagoFormat: OFormat[StopMonitorChicago] =
    Json.format[StopMonitorChicago]
  implicit val StartMonitorRubberFormat: OFormat[StartMonitorRubber] =
    Json.format[StartMonitorRubber]
  implicit val StopMonitorRubberFormat: OFormat[StopMonitorRubber] =
    Json.format[StopMonitorRubber]

}

abstract class SealedFormat[T](implicit baseclass: ClassTag[T])
    extends Format[T] {
  import ProtocolJsonSupportImpl._

  val baseclassname = baseclass.runtimeClass.getName

  /**
    * @return None if className is unknown
    */
  def unmarshall(className: String, value: JsObject): Option[JsResult[T]]
  def marshall(obj: T): JsValue

  import scala.language.implicitConversions
  implicit def toT[A <: T](a: A): T = a

  def reads(json: JsValue): JsResult[T] = {
    json match {
      case JsObject(map) =>
        map.get(classNameKey) match {
          case Some(JsString(className)) =>
            map.get(dataKey) match {
              case Some(JsNull) =>
                JsSuccess(null.asInstanceOf[T])
              case Some(o: JsObject) =>
                unmarshall(className, o) match {
                  case Some(res) => res
                  case None =>
                    JsError(
                      JsonValidationError(
                        Seq(
                          s"Reading ${baseclassname}: Unknown class ${className}: ${Json.stringify(json)}"
                        )
                      )
                    )
                }
              case Some(x) =>
                JsError(
                  JsonValidationError(
                    Seq(
                      s"Reading ${baseclassname}: Expecting object or null for ${dataKey}: ${Json.stringify(x)}"
                    )
                  )
                )
              case None =>
                JsError(
                  JsonValidationError(
                    Seq(
                      s"Reading ${baseclassname}: Missing key ${dataKey}: ${Json.stringify(json)}"
                    )
                  )
                )
            }
          case Some(x) =>
            JsError(
              JsonValidationError(
                Seq(
                  s"Reading ${baseclassname}: Expecting string for ${classNameKey}: ${Json.stringify(x)}"
                )
              )
            )
          case None =>
            JsError(
              JsonValidationError(
                Seq(
                  s"Reading ${baseclassname}: Missing key ${classNameKey}: ${Json.stringify(json)}"
                )
              )
            )
        }
      case _ =>
        JsError(
          JsonValidationError(
            Seq(
              "Reading ${baseclassname}: Expecting a JSON object, got ${json.getClass.getSimpleName}: ${json}"
            )
          )
        )
    }
  }

  def writes(o: T): JsValue = {
    Json.obj(
      classNameKey -> JsString(o.getClass.getName),
      dataKey -> marshall(o)
    )
  }
}

class ToServerMessageFormat extends SealedFormat[ToServerMessage] {
  import ProtocolJsonSupportImpl._

  /**
    * @return None if class name is unknown
    */
  def unmarshall(
      className: String,
      o: JsObject
  ): Option[JsResult[ToServerMessage]] = {
    if (className == classOf[StartMonitorSummary].getName) {
      Option(Json.fromJson[StartMonitorSummary](o).map { o =>
        o
      })
    } else if (className == classOf[StopMonitorSummary].getName) {
      Option(Json.fromJson[StopMonitorSummary](o).map { o =>
        o
      })
    } else if (className == classOf[StartMonitorDuplicate].getName) {
      Option(Json.fromJson[StartMonitorDuplicate](o).map { o =>
        o
      })
    } else if (className == classOf[StopMonitorDuplicate].getName) {
      Option(Json.fromJson[StopMonitorDuplicate](o).map { o =>
        o
      })
    } else if (className == classOf[StartMonitorIndividualDuplicate].getName) {
      Option(Json.fromJson[StartMonitorIndividualDuplicate](o).map { o =>
        o
      })
    } else if (className == classOf[StopMonitorIndividualDuplicate].getName) {
      Option(Json.fromJson[StopMonitorIndividualDuplicate](o).map { o =>
        o
      })
    } else if (className == classOf[StartMonitorChicago].getName) {
      Option(Json.fromJson[StartMonitorChicago](o).map { o =>
        o
      })
    } else if (className == classOf[StopMonitorChicago].getName) {
      Option(Json.fromJson[StopMonitorChicago](o).map { o =>
        o
      })
    } else if (className == classOf[StartMonitorRubber].getName) {
      Option(Json.fromJson[StartMonitorRubber](o).map { o =>
        o
      })
    } else if (className == classOf[StopMonitorRubber].getName) {
      Option(Json.fromJson[StopMonitorRubber](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateDuplicate].getName) {
      Option(Json.fromJson[UpdateDuplicate](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateDuplicateHand].getName) {
      Option(Json.fromJson[UpdateDuplicateHand](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateDuplicateTeam].getName) {
      Option(Json.fromJson[UpdateDuplicateTeam](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateDuplicatePicture].getName) {
      Option(Json.fromJson[UpdateDuplicatePicture](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateDuplicatePictures].getName) {
      Option(Json.fromJson[UpdateDuplicatePictures](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateIndividualDuplicate].getName) {
      Option(Json.fromJson[UpdateIndividualDuplicate](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateIndividualDuplicateHand].getName) {
      Option(Json.fromJson[UpdateIndividualDuplicateHand](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateIndividualDuplicatePicture].getName) {
      Option(Json.fromJson[UpdateIndividualDuplicatePicture](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateIndividualDuplicatePictures].getName) {
      Option(Json.fromJson[UpdateIndividualDuplicatePictures](o).map { o =>
        o
      })
    } else if (className == classOf[NoData].getName) {
      Option(Json.fromJson[NoData](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateChicago].getName) {
      Option(Json.fromJson[UpdateChicago](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateChicagoRound].getName) {
      Option(Json.fromJson[UpdateChicagoRound](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateChicagoHand].getName) {
      Option(Json.fromJson[UpdateChicagoHand](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateRubber].getName) {
      Option(Json.fromJson[UpdateRubber](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateRubberHand].getName) {
      Option(Json.fromJson[UpdateRubberHand](o).map { o =>
        o
      })
    } else {
      None
    }
  }

  def marshall(obj: ToServerMessage): JsValue = {
    obj match {
      case x: StartMonitorSummary     => Json.toJson(x)
      case x: StopMonitorSummary      => Json.toJson(x)
      case x: StartMonitorDuplicate   => Json.toJson(x)
      case x: StopMonitorDuplicate    => Json.toJson(x)
      case x: StartMonitorIndividualDuplicate   => Json.toJson(x)
      case x: StopMonitorIndividualDuplicate    => Json.toJson(x)
      case x: StartMonitorChicago     => Json.toJson(x)
      case x: StopMonitorChicago      => Json.toJson(x)
      case x: StartMonitorRubber      => Json.toJson(x)
      case x: StopMonitorRubber       => Json.toJson(x)
      case x: UpdateDuplicate         => Json.toJson(x)
      case x: UpdateDuplicateHand     => Json.toJson(x)
      case x: UpdateDuplicateTeam     => Json.toJson(x)
      case x: UpdateDuplicatePicture  => Json.toJson(x)
      case x: UpdateDuplicatePictures => Json.toJson(x)
      case x: UpdateIndividualDuplicate         => Json.toJson(x)
      case x: UpdateIndividualDuplicateHand     => Json.toJson(x)
      case x: UpdateIndividualDuplicatePicture  => Json.toJson(x)
      case x: UpdateIndividualDuplicatePictures => Json.toJson(x)
      case x: NoData                  => Json.toJson(x)
      case x: UpdateChicago           => Json.toJson(x)
      case x: UpdateChicagoRound      => Json.toJson(x)
      case x: UpdateChicagoHand       => Json.toJson(x)
      case x: UpdateRubber            => Json.toJson(x)
      case x: UpdateRubberHand        => Json.toJson(x)
    }
  }
}

class ToBrowserMessageFormat extends SealedFormat[ToBrowserMessage] {
  import ProtocolJsonSupportImpl._

  /**
    * @return None if class name is unknown
    */
  def unmarshall(
      className: String,
      o: JsObject
  ): Option[JsResult[ToBrowserMessage]] = {
    if (className == classOf[MonitorJoined].getName) {
      Option(Json.fromJson[MonitorJoined](o).map { o =>
        o
      })
    } else if (className == classOf[MonitorLeft].getName) {
      Option(Json.fromJson[MonitorLeft](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateDuplicate].getName) {
      Option(Json.fromJson[UpdateDuplicate](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateDuplicateHand].getName) {
      Option(Json.fromJson[UpdateDuplicateHand](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateDuplicateTeam].getName) {
      Option(Json.fromJson[UpdateDuplicateTeam](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateDuplicatePicture].getName) {
      Option(Json.fromJson[UpdateDuplicatePicture](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateDuplicatePictures].getName) {
      Option(Json.fromJson[UpdateDuplicatePictures](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateIndividualDuplicate].getName) {
      Option(Json.fromJson[UpdateIndividualDuplicate](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateIndividualDuplicateHand].getName) {
      Option(Json.fromJson[UpdateIndividualDuplicateHand](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateIndividualDuplicatePicture].getName) {
      Option(Json.fromJson[UpdateIndividualDuplicatePicture](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateIndividualDuplicatePictures].getName) {
      Option(Json.fromJson[UpdateIndividualDuplicatePictures](o).map { o =>
        o
      })
    } else if (className == classOf[NoData].getName) {
      Option(Json.fromJson[NoData](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateChicago].getName) {
      Option(Json.fromJson[UpdateChicago](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateChicagoRound].getName) {
      Option(Json.fromJson[UpdateChicagoRound](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateChicagoHand].getName) {
      Option(Json.fromJson[UpdateChicagoHand](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateRubber].getName) {
      Option(Json.fromJson[UpdateRubber](o).map { o =>
        o
      })
    } else if (className == classOf[UpdateRubberHand].getName) {
      Option(Json.fromJson[UpdateRubberHand](o).map { o =>
        o
      })
    } else {
      None
    }
  }

  def marshall(obj: ToBrowserMessage): JsValue = {
    obj match {
      case x: MonitorJoined           => Json.toJson[MonitorJoined](x)
      case x: MonitorLeft             => Json.toJson[MonitorLeft](x)
      case x: UpdateDuplicate         => Json.toJson[UpdateDuplicate](x)
      case x: UpdateDuplicateHand     => Json.toJson[UpdateDuplicateHand](x)
      case x: UpdateDuplicateTeam     => Json.toJson[UpdateDuplicateTeam](x)
      case x: UpdateDuplicatePicture  => Json.toJson[UpdateDuplicatePicture](x)
      case x: UpdateDuplicatePictures => Json.toJson[UpdateDuplicatePictures](x)
      case x: UpdateIndividualDuplicate         => Json.toJson[UpdateIndividualDuplicate](x)
      case x: UpdateIndividualDuplicateHand     => Json.toJson[UpdateIndividualDuplicateHand](x)
      case x: UpdateIndividualDuplicatePicture  => Json.toJson[UpdateIndividualDuplicatePicture](x)
      case x: UpdateIndividualDuplicatePictures => Json.toJson[UpdateIndividualDuplicatePictures](x)
      case x: NoData                  => Json.toJson[NoData](x)
      case x: UpdateChicago           => Json.toJson[UpdateChicago](x)
      case x: UpdateChicagoRound      => Json.toJson(x)
      case x: UpdateChicagoHand       => Json.toJson(x)
      case x: UpdateRubber            => Json.toJson(x)
      case x: UpdateRubberHand        => Json.toJson(x)
    }
  }
}

trait ToBrowserProtocolJsonSupport {
  implicit val toBrowserMessageFormat: ToBrowserMessageFormat =
    new ToBrowserMessageFormat // Json.format[ToBrowserMessage]
}

trait ToServerProtocolJsonSupport {
  implicit val toServerMessageFormat: ToServerMessageFormat =
    new ToServerMessageFormat // Json.format[ToServerMessage]
}

object ToBrowserProtocolJsonSupport extends ToBrowserProtocolJsonSupport
object ToServerProtocolJsonSupport extends ToServerProtocolJsonSupport
