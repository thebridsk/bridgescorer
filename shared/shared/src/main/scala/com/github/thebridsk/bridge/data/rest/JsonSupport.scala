package com.github.thebridsk.bridge.data.rest

import play.api.libs.json._
import com.github.thebridsk.bridge.data._
import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.LogEntryV2
import com.github.thebridsk.bridge.data.duplicate.suggestion.Suggestion
import com.github.thebridsk.bridge.data.duplicate.suggestion.DuplicateSuggestions
import com.github.thebridsk.bridge.data.duplicate.suggestion.Pairing
import com.github.thebridsk.bridge.data.duplicate.suggestion.NeverPair
import com.github.thebridsk.bridge.data.duplicate.stats.CounterStat
import com.github.thebridsk.bridge.data.duplicate.stats.ContractStat
import com.github.thebridsk.bridge.data.duplicate.stats.ContractStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerStat
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerStats
import com.github.thebridsk.bridge.data.duplicate.stats.DuplicateStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStat
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayersOpponentsStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerOpponentStat
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerOpponentsStat
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlace
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerPlaces
import scala.reflect.ClassTag

//import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.{ LogEntry => DpLogEntry, _ }
//import com.github.thebridsk.bridge.data.websocket.Protocol._

class UnitFormat extends Format[Unit] {

  def unit = {}

  def reads(json: JsValue): JsResult[Unit] = {
    JsSuccess(unit)
  }

  def writes(o: Unit): JsValue = {
    JsNull
  }
}

trait JsonSupport {

  implicit val idMatchRubberKeyReads = MatchRubber.idKeyReads
  implicit val idMatchRubberKeyWrites = MatchRubber.idKeyWrites
  implicit val idMatchChicagoKeyReads = MatchChicago.idKeyReads
  implicit val idMatchChicagoKeyWrites = MatchChicago.idKeyWrites
  implicit val idDuplicateMatchKeyReads = DuplicateSummary.idKeyReads
  implicit val idDuplicateMatchKeyWrites = DuplicateSummary.idKeyWrites
  implicit val idMatchDuplicateKeyReads = MatchDuplicate.idKeyReads
  implicit val idMatchDuplicateKeyWrites = MatchDuplicate.idKeyWrites
  implicit val idBoardKeyReads = Board.idKeyReads
  implicit val idBoardKeyWrites = Board.idKeyWrites
  implicit val idTeamKeyReads = Team.idKeyReads
  implicit val idTeamKeyWrites = Team.idKeyWrites
  implicit val idTableKeyReads = Table.idKeyReads
  implicit val idTableKeyWrites = Table.idKeyWrites
  implicit val idBoardSetKeyReads = BoardSet.idKeyReads
  implicit val idBoardSetKeyWrites = BoardSet.idKeyWrites
  implicit val idMovementKeyReads = Movement.idKeyReads
  implicit val idMovementKeyWrites = Movement.idKeyWrites
  implicit val idMatchDuplicateResultKeyReads = MatchDuplicateResult.idKeyReads
  implicit val idMatchDuplicateResultKeyWrites = MatchDuplicateResult.idKeyWrites

  implicit val idMatchRubberFormat         = MatchRubber.jsonFormat
  implicit val idMatchChicagoFormat         = MatchChicago.jsonFormat
  implicit val idDuplicateMatchFormat       = DuplicateSummary.jsonFormat
  implicit val idMatchDuplicateFormat       = MatchDuplicate.jsonFormat
  implicit val idMatchDuplicateBoardFormat  = Board.jsonFormat
  implicit val idMatchDuplicateTeamFormat   = Team.jsonFormat
  implicit val idMatchDuplicateTableFormat  = Table.jsonFormat
  implicit val idBoardSetFormat = BoardSet.jsonFormat
  implicit val idMovementFormat = Movement.jsonFormat
  implicit val idMatchDuplicateResultFormat = MatchDuplicateResult.jsonFormat

  implicit val tableFormat = Json.format[Table]
  implicit val teamFormat = Json.format[Team]
  implicit val handFormat = Json.format[Hand]
  implicit val rubberHandFormat = Json.format[RubberHand]
  implicit val roundFormat = Json.format[Round]
//  implicit val duplicateHandFormat = Json.format[DuplicateHand]
  implicit val duplicateHandV1Format = Json.format[DuplicateHandV1]
  implicit val duplicateHandV2Format = Json.format[DuplicateHandV2]
//  implicit val boardFormat = Json.format[Board]
  implicit val boardv1Format = Json.format[BoardV1]
  implicit val boardv2Format = Json.format[BoardV2]
//  implicit val duplicateFormat = Json.format[MatchDuplicate]
  implicit val duplicateV1Format = Json.format[MatchDuplicateV1]
  implicit val duplicateV2Format = Json.format[MatchDuplicateV2]
  implicit val duplicateV3Format = Json.format[MatchDuplicateV3]
  implicit val duplicateSummaryDetailsFormat =
    Json.format[DuplicateSummaryDetails]
  implicit val duplicateSummaryEntryFormat = Json.format[DuplicateSummaryEntry]
  implicit val bestMatchFormat = Json.format[BestMatch]
  implicit val duplicateSummaryFormat = Json.format[DuplicateSummary]
  implicit val boardTeamResultsFormat = Json.format[BoardTeamResults]
  implicit val boardResultsFormat = Json.format[BoardResults]
  implicit val duplicatePictureFormat = Json.format[DuplicatePicture]
//  implicit val duplicateResultFormat = Json.format[MatchDuplicateResult]
  implicit val duplicateResultV1Format = Json.format[MatchDuplicateResultV1]
  implicit val duplicateResultV2Format = Json.format[MatchDuplicateResultV2]
  implicit val chicagoBestMatchFormat = Json.format[ChicagoBestMatch]
  implicit val chicagoFormat = Json.format[MatchChicago]
  implicit val chicagov2Format = Json.format[MatchChicagoV2]
  implicit val chicagov1Format = Json.format[MatchChicagoV1]
  implicit val rubberBestMatchFormat = Json.format[RubberBestMatch]
  implicit val rubberFormat = Json.format[MatchRubber]
  implicit val loggerConfigFormat = Json.format[LoggerConfig]
  implicit val serverVersionFormat = Json.format[ServerVersion]
  implicit val serverURLFormat = Json.format[ServerURL]
  implicit val ackFormat = Json.format[Ack]
  implicit val restErrorFormat = Json.format[RestMessage]
  implicit val boardInSetFormat = Json.format[BoardInSet]
  implicit val boardSetFormat = Json.format[BoardSet]
  implicit val handInTableFormat = Json.format[HandInTable]
  implicit val movementFormat = Json.format[Movement]
  implicit val loggerFormat = Json.format[LogEntryV2]
//  implicit val boarsetsAndMovementsFormat = Json.format[BoardSetsAndMovementsV1]
  implicit val boarsetsAndMovementsV1Format =
    Json.format[BoardSetsAndMovementsV1]

  implicit val pairingFormat = Json.format[Pairing]
  implicit val suggestionFormat = Json.format[Suggestion]
  implicit val duplicateNeverPairFormat = Json.format[NeverPair]
  implicit val duplicateSuggestionsFormat = Json.format[DuplicateSuggestions]
  implicit val duplicatePlayerPlaceFormat = Json.format[PlayerPlace]
  implicit val duplicatePlayerPlacesFormat = Json.format[PlayerPlaces]

  implicit val counterStatFormat = Json.format[CounterStat]
  implicit val contractStatFormat = Json.format[ContractStat]
  implicit val contractStatsFormat = Json.format[ContractStats]
  implicit val playerStatFormat = Json.format[PlayerStat]
  implicit val playerStatsFormat = Json.format[PlayerStats]
  implicit val playerComparisonStatFormat = Json.format[PlayerComparisonStat]
  implicit val playerComparisonStatsFormat = Json.format[PlayerComparisonStats]
  implicit val playerOpponentStatFormat = Json.format[PlayerOpponentStat]
  implicit val playerOpponentsStatFormat = Json.format[PlayerOpponentsStat]
  implicit val playersOpponentsStatsFormat = Json.format[PlayersOpponentsStats]
  implicit val duplicateStatsFormat = Json.format[DuplicateStats]

  implicit val importStoreData = Json.format[ImportStoreData]

  implicit val unitFormat = new UnitFormat

  def readJson[T](s: String)(implicit reader: Reads[T]): T = {
    val json = Json.parse(s)
    convertJson[T](json)
  }

  def convertJson[T](jsvalue: JsValue)(implicit reads: Reads[T]): T = {
    Json.fromJson[T](jsvalue) match {
      case JsSuccess(value, path) =>
        value
      case e: JsError =>
        println("Errors: " + JsError.toJson(e).toString())
        throw new JsonException(
          s"JSON error on ${jsvalue.getClass.getName}: ${JsError.toJson(e)}"
        )
    }
  }

  def writeJson[T](t: T)(implicit writer: Writes[T]): String = {
    val json = Json.toJson(t)
    Json.stringify(json)
  }

  def writePrettyJson[T](t: T)(implicit writer: Writes[T]): String = {
    val json = Json.toJson(t)
    Json.prettyPrint(json)
  }

}

object JsonSupport extends JsonSupport {

  implicit class ToJsonSupportWrapper[T](val t: T) extends AnyVal {
    def toJson(implicit writer: Writes[T]) = writeJson(t)
    def toJsonPretty(implicit writer: Writes[T]) = writePrettyJson(t)
  }

  implicit class FromJsonSupportWrapper(val json: String) extends AnyVal {
    def parseJson[T](implicit reader: Reads[T]): T = readJson[T](json)
  }

}
