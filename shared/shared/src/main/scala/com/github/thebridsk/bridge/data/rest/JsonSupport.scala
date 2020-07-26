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

//import com.github.thebridsk.bridge.data.websocket.DuplexProtocol.{ LogEntry => DpLogEntry, _ }
//import com.github.thebridsk.bridge.data.websocket.Protocol._

class UnitFormat extends Format[Unit] {

  def unit: Unit = {}

  def reads(json: JsValue): JsResult[Unit] = {
    JsSuccess(unit)
  }

  def writes(o: Unit): JsValue = {
    JsNull
  }
}

trait JsonSupport {

  implicit val idMatchRubberKeyReads: KeyReads[Id[IdMatchRubber]] = MatchRubber.idKeyReads
  implicit val idMatchRubberKeyWrites: KeyWrites[Id[IdMatchRubber]] = MatchRubber.idKeyWrites
  implicit val idMatchChicagoKeyReads: KeyReads[Id[IdMatchChicago]] = MatchChicago.idKeyReads
  implicit val idMatchChicagoKeyWrites: KeyWrites[Id[IdMatchChicago]] = MatchChicago.idKeyWrites
  implicit val idDuplicateMatchKeyReads: KeyReads[Id[IdDuplicateSummary]] = DuplicateSummary.idKeyReads
  implicit val idDuplicateMatchKeyWrites: KeyWrites[Id[IdDuplicateSummary]] = DuplicateSummary.idKeyWrites
  implicit val idMatchDuplicateKeyReads: KeyReads[Id[IdMatchDuplicate]] = MatchDuplicate.idKeyReads
  implicit val idMatchDuplicateKeyWrites: KeyWrites[Id[IdMatchDuplicate]] = MatchDuplicate.idKeyWrites
  implicit val idBoardKeyReads: KeyReads[Id[IdBoard]] = Board.idKeyReads
  implicit val idBoardKeyWrites: KeyWrites[Id[IdBoard]] = Board.idKeyWrites
  implicit val idTeamKeyReads: KeyReads[Id[IdTeam]] = Team.idKeyReads
  implicit val idTeamKeyWrites: KeyWrites[Id[IdTeam]] = Team.idKeyWrites
  implicit val idTableKeyReads: KeyReads[Id[IdTable]] = Table.idKeyReads
  implicit val idTableKeyWrites: KeyWrites[Id[IdTable]] = Table.idKeyWrites
  implicit val idBoardSetKeyReads: KeyReads[Id[IdBoardSet]] = BoardSet.idKeyReads
  implicit val idBoardSetKeyWrites: KeyWrites[Id[IdBoardSet]] = BoardSet.idKeyWrites
  implicit val idMovementKeyReads: KeyReads[Id[IdMovement]] = Movement.idKeyReads
  implicit val idMovementKeyWrites: KeyWrites[Id[IdMovement]] = Movement.idKeyWrites
  implicit val idMatchDuplicateResultKeyReads: KeyReads[Id[IdMatchDuplicateResult]] = MatchDuplicateResult.idKeyReads
  implicit val idMatchDuplicateResultKeyWrites: KeyWrites[Id[IdMatchDuplicateResult]] = MatchDuplicateResult.idKeyWrites

  implicit val idMatchRubberFormat: Format[Id[IdMatchRubber]]         = MatchRubber.jsonFormat
  implicit val idMatchChicagoFormat: Format[Id[IdMatchChicago]]         = MatchChicago.jsonFormat
  implicit val idDuplicateMatchFormat: Format[Id[IdDuplicateSummary]]       = DuplicateSummary.jsonFormat
  implicit val idMatchDuplicateFormat: Format[Id[IdMatchDuplicate]]       = MatchDuplicate.jsonFormat
  implicit val idMatchDuplicateBoardFormat: Format[Id[IdBoard]]  = Board.jsonFormat
  implicit val idMatchDuplicateTeamFormat: Format[Id[IdTeam]]   = Team.jsonFormat
  implicit val idMatchDuplicateTableFormat: Format[Id[IdTable]]  = Table.jsonFormat
  implicit val idBoardSetFormat: Format[Id[IdBoardSet]] = BoardSet.jsonFormat
  implicit val idMovementFormat: Format[Id[IdMovement]] = Movement.jsonFormat
  implicit val idMatchDuplicateResultFormat: Format[Id[IdMatchDuplicateResult]] = MatchDuplicateResult.jsonFormat

  implicit val tableFormat: OFormat[Table] = Json.format[Table]
  implicit val teamFormat: OFormat[Team] = Json.format[Team]
  implicit val handFormat: OFormat[Hand] = Json.format[Hand]
  implicit val rubberHandFormat: OFormat[RubberHand] = Json.format[RubberHand]
  implicit val roundFormat: OFormat[Round] = Json.format[Round]
//  implicit val duplicateHandFormat = Json.format[DuplicateHand]
  implicit val duplicateHandV1Format: OFormat[DuplicateHandV1] = Json.format[DuplicateHandV1]
  implicit val duplicateHandV2Format: OFormat[DuplicateHandV2] = Json.format[DuplicateHandV2]
//  implicit val boardFormat = Json.format[Board]
  implicit val boardv1Format: OFormat[BoardV1] = Json.format[BoardV1]
  implicit val boardv2Format: OFormat[BoardV2] = Json.format[BoardV2]
//  implicit val duplicateFormat = Json.format[MatchDuplicate]
  implicit val duplicateV1Format: OFormat[MatchDuplicateV1] = Json.format[MatchDuplicateV1]
  implicit val duplicateV2Format: OFormat[MatchDuplicateV2] = Json.format[MatchDuplicateV2]
  implicit val duplicateV3Format: OFormat[MatchDuplicateV3] = Json.format[MatchDuplicateV3]
  implicit val duplicateSummaryDetailsFormat: OFormat[DuplicateSummaryDetails] =
    Json.format[DuplicateSummaryDetails]
  implicit val duplicateSummaryEntryFormat: OFormat[DuplicateSummaryEntry] = Json.format[DuplicateSummaryEntry]
  implicit val bestMatchFormat: OFormat[BestMatch] = Json.format[BestMatch]
  implicit val duplicateSummaryFormat: OFormat[DuplicateSummary] = Json.format[DuplicateSummary]
  implicit val boardTeamResultsFormat: OFormat[BoardTeamResults] = Json.format[BoardTeamResults]
  implicit val boardResultsFormat: OFormat[BoardResults] = Json.format[BoardResults]
  implicit val duplicatePictureFormat: OFormat[DuplicatePicture] = Json.format[DuplicatePicture]
//  implicit val duplicateResultFormat = Json.format[MatchDuplicateResult]
  implicit val duplicateResultV1Format: OFormat[MatchDuplicateResultV1] = Json.format[MatchDuplicateResultV1]
  implicit val duplicateResultV2Format: OFormat[MatchDuplicateResultV2] = Json.format[MatchDuplicateResultV2]
  implicit val chicagoBestMatchFormat: OFormat[ChicagoBestMatch] = Json.format[ChicagoBestMatch]
  implicit val chicagoFormat: OFormat[MatchChicago] = Json.format[MatchChicago]
  implicit val chicagov2Format: OFormat[MatchChicagoV2] = Json.format[MatchChicagoV2]
  implicit val chicagov1Format: OFormat[MatchChicagoV1] = Json.format[MatchChicagoV1]
  implicit val rubberBestMatchFormat: OFormat[RubberBestMatch] = Json.format[RubberBestMatch]
  implicit val rubberFormat: OFormat[MatchRubber] = Json.format[MatchRubber]
  implicit val loggerConfigFormat: OFormat[LoggerConfig] = Json.format[LoggerConfig]
  implicit val serverVersionFormat: OFormat[ServerVersion] = Json.format[ServerVersion]
  implicit val serverURLFormat: OFormat[ServerURL] = Json.format[ServerURL]
  implicit val ackFormat: OFormat[Ack] = Json.format[Ack]
  implicit val restErrorFormat: OFormat[RestMessage] = Json.format[RestMessage]
  implicit val boardInSetFormat: OFormat[BoardInSet] = Json.format[BoardInSet]
  implicit val boardSetFormat: OFormat[BoardSet] = Json.format[BoardSet]
  implicit val handInTableFormat: OFormat[HandInTable] = Json.format[HandInTable]
  implicit val movementFormat: OFormat[Movement] = Json.format[Movement]
  implicit val loggerFormat: OFormat[LogEntryV2] = Json.format[LogEntryV2]
//  implicit val boarsetsAndMovementsFormat = Json.format[BoardSetsAndMovementsV1]
  implicit val boarsetsAndMovementsV1Format: OFormat[BoardSetsAndMovementsV1] =
    Json.format[BoardSetsAndMovementsV1]

  implicit val pairingFormat: OFormat[Pairing] = Json.format[Pairing]
  implicit val suggestionFormat: OFormat[Suggestion] = Json.format[Suggestion]
  implicit val duplicateNeverPairFormat: OFormat[NeverPair] = Json.format[NeverPair]
  implicit val duplicateSuggestionsFormat: OFormat[DuplicateSuggestions] = Json.format[DuplicateSuggestions]
  implicit val duplicatePlayerPlaceFormat: OFormat[PlayerPlace] = Json.format[PlayerPlace]
  implicit val duplicatePlayerPlacesFormat: OFormat[PlayerPlaces] = Json.format[PlayerPlaces]

  implicit val counterStatFormat: OFormat[CounterStat] = Json.format[CounterStat]
  implicit val contractStatFormat: OFormat[ContractStat] = Json.format[ContractStat]
  implicit val contractStatsFormat: OFormat[ContractStats] = Json.format[ContractStats]
  implicit val playerStatFormat: OFormat[PlayerStat] = Json.format[PlayerStat]
  implicit val playerStatsFormat: OFormat[PlayerStats] = Json.format[PlayerStats]
  implicit val playerComparisonStatFormat: OFormat[PlayerComparisonStat] = Json.format[PlayerComparisonStat]
  implicit val playerComparisonStatsFormat: OFormat[PlayerComparisonStats] = Json.format[PlayerComparisonStats]
  implicit val playerOpponentStatFormat: OFormat[PlayerOpponentStat] = Json.format[PlayerOpponentStat]
  implicit val playerOpponentsStatFormat: OFormat[PlayerOpponentsStat] = Json.format[PlayerOpponentsStat]
  implicit val playersOpponentsStatsFormat: OFormat[PlayersOpponentsStats] = Json.format[PlayersOpponentsStats]
  implicit val duplicateStatsFormat: OFormat[DuplicateStats] = Json.format[DuplicateStats]

  implicit val importStoreData: OFormat[ImportStoreData] = Json.format[ImportStoreData]

  implicit val unitFormat: UnitFormat = new UnitFormat

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

  implicit class ToJsonSupportWrapper[T](private val t: T) extends AnyVal {
    def toJson(implicit writer: Writes[T]): String = writeJson(t)
    def toJsonPretty(implicit writer: Writes[T]): String = writePrettyJson(t)
  }

  implicit class FromJsonSupportWrapper(private val json: String) extends AnyVal {
    def parseJson[T](implicit reader: Reads[T]): T = readJson[T](json)
  }

}
