package com.github.thebridsk.bridge.server.service.graphql.schema

import sangria.schema._
import com.github.thebridsk.bridge.server.backend.BridgeService
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.server.service.graphql.Data.ImportBridgeService
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Hand
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.BridgeService

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.bridge.data.Id
import sangria.validation.ValueCoercionViolation
import sangria.ast.ScalarValue
import sangria.ast
import com.github.thebridsk.bridge.data.SystemTime.Timestamp
import com.github.thebridsk.bridge.data.DuplicateSummary
import com.github.thebridsk.bridge.data.DuplicateSummaryEntry
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.BestMatch
import com.github.thebridsk.bridge.data.BestMatch
import com.github.thebridsk.bridge.data.Difference
import com.github.thebridsk.bridge.data.DifferenceWrappers
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.BoardResults
import com.github.thebridsk.bridge.data.BoardTeamResults
import sangria.ast.AstLocation
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerStat
import com.github.thebridsk.bridge.data.duplicate.stats.CounterStat
import com.github.thebridsk.bridge.data.duplicate.stats.ContractStat
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerStats
import com.github.thebridsk.bridge.data.duplicate.stats.ContractStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerDoubledStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStats
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerComparisonStat
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.bridge.East
import com.github.thebridsk.bridge.data.bridge.West
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.ChicagoBestMatch
import com.github.thebridsk.bridge.data.RubberBestMatch

import SchemaBase.{log => _, _}
import SchemaHand.{log => _, _}
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerOpponentStat
import com.github.thebridsk.bridge.data.duplicate.stats.PlayerOpponentsStat
import com.github.thebridsk.bridge.data.duplicate.stats.PlayersOpponentsStats
import com.github.thebridsk.bridge.data.Table

object SchemaDuplicate {

  val log = Logger(SchemaDuplicate.getClass.getName)

  val TeamIdType = idScalarType[Team.Type]("TeamId", Team)
  val TableIdType = idScalarType[Table.Type]("TableId", Table)
  val BoardIdType = idScalarTypeFromString[Id.DuplicateBoard]("BoardId")
  val DuplicateIdType = idScalarTypeFromString[Id.MatchDuplicate]("DuplicateId")
  val DuplicateResultIdType =
    idScalarTypeFromString[Id.MatchDuplicateResult]("DuplicateResultId")

  val DuplicateTeamType = ObjectType(
    "DuplicateTeam",
    "A duplicate team",
    fields[BridgeService, Team](
      Field("id", TeamIdType, Some("The id of the team"), resolve = _.value.id),
      Field(
        "player1",
        StringType,
        Some("The name of player 1"),
        resolve = _.value.player1
      ),
      Field(
        "player2",
        StringType,
        Some("The name of player 2"),
        resolve = _.value.player2
      ),
      Field(
        "created",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value.created
      ),
      Field(
        "updated",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value.updated
      )
    )
  )

  val DuplicateHandType = ObjectType(
    "DuplicateHand",
    "A duplicate hand",
    fields[BridgeService, DuplicateHand](
      Field(
        "id",
        TeamIdType,
        Some("The id of the NS team"),
        resolve = _.value.id
      ),
      Field(
        "played",
        ListType(HandType),
        Some("The played hand"),
        resolve = _.value.played
      ),
      Field(
        "table",
        TableIdType,
        Some("The table the hand was played on"),
        resolve = _.value.table
      ),
      Field(
        "round",
        IntType,
        Some("The round the hand was played in"),
        resolve = _.value.round
      ),
      Field("board", StringType, Some("The board"), resolve = _.value.board),
      Field(
        "nsTeam",
        TeamIdType,
        Some("The NS team that plays the hand"),
        resolve = _.value.nsTeam
      ),
      Field(
        "nIsPlayer1",
        BooleanType,
        Some("The north player is player 1 of the team"),
        resolve = _.value.nIsPlayer1
      ),
      Field(
        "ewTeam",
        TeamIdType,
        Some("The EW team that plays the hand"),
        resolve = _.value.ewTeam
      ),
      Field(
        "eIsPlayer1",
        BooleanType,
        Some("The east player is player 1 of the team"),
        resolve = _.value.eIsPlayer1
      ),
      Field(
        "created",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value.created
      ),
      Field(
        "updated",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value.updated
      )
    )
  )

  val ArgHandId = Argument(
    "id",
    TeamIdType,
    description = "The Id of the hand.  This the team id of the NS team."
  )

  val DuplicateBoardType = ObjectType(
    "DuplicateBoard",
    "A duplicate board",
    fields[BridgeService, Board](
      Field(
        "id",
        DuplicateIdType,
        Some("The id of the board"),
        resolve = _.value.id
      ),
      Field(
        "nsVul",
        BooleanType,
        Some("The vulnerability of NS for the contract."),
        resolve = _.value.nsVul
      ),
      Field(
        "ewVul",
        BooleanType,
        Some("The vulnerability of EW for the contract."),
        resolve = _.value.ewVul
      ),
      Field("dealer", StringType, Some("The dealer"), resolve = _.value.dealer),
      Field(
        "hands",
        ListType(DuplicateHandType),
        Some("The played hand"),
        resolve = _.value.hands
      ),
      Field(
        "hand",
        OptionType(DuplicateHandType),
        Some("The played hand"),
        arguments = ArgHandId :: Nil,
        resolve = ctx => {
          val id = ctx arg ArgHandId
          ctx.value.getHand(id)
        }
      ),
      Field(
        "created",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value.created
      ),
      Field(
        "updated",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value.updated
      )
    )
  )

  val ArgTeamId =
    Argument("id", TeamIdType, description = "The Id of the team.")

  val ArgBoardId =
    Argument("id", BoardIdType, description = "The Id of the board.")

  val DuplicateSummaryTeamType = ObjectType(
    "DuplicateSummaryTeam",
    "A team result in a DuplicateSummary",
    fields[BridgeService, DuplicateSummaryEntry](
      Field(
        "id",
        TeamIdType,
        Some("The id of the duplicate match"),
        resolve = _.value.team.id
      ),
      Field(
        "team",
        DuplicateTeamType,
        Some("The team"),
        resolve = _.value.team
      ),
      Field(
        "result",
        OptionType(FloatType),
        Some("The points the team scored"),
        resolve = _.value.result
      ),
      Field(
        "place",
        OptionType(IntType),
        Some("The place the team finished in"),
        resolve = _.value.place
      ),
      Field(
        "resultIMP",
        OptionType(FloatType),
        Some("The IMPs the team scored"),
        resolve = _.value.resultImp
      ),
      Field(
        "placeIMP",
        OptionType(IntType),
        Some("The place the team finished in using IMPs"),
        resolve = _.value.placeImp
      )
    )
  )

  val BestMatchType = ObjectType(
    "BestMatch",
    "Identifies the best match",
    fields[BridgeService, (Option[String], BestMatch)](
      Field(
        "id",
        OptionType(DuplicateIdType),
        Some("The id of the best duplicate match from the main store"),
        resolve = _.value._2.id
      ),
      Field(
        "sameness",
        FloatType,
        Some("A percentage of similarity."),
        resolve = _.value._2.sameness
      ),
      Field(
        "differences",
        OptionType(ListType(StringType)),
        Some("The fields that are different"),
        resolve = _.value._2.differences
      )
    )
  )

  val DuplicateSummaryType = ObjectType(
    "DuplicateSummary",
    "A duplicate match",
    fields[BridgeService, (Option[String], DuplicateSummary)](
      Field(
        "id",
        DuplicateIdType,
        Some("The id of the duplicate match"),
        resolve = _.value._2.id
      ),
      Field(
        "finished",
        BooleanType,
        Some("true if the match is finished"),
        resolve = _.value._2.finished
      ),
      Field(
        "teams",
        ListType(DuplicateSummaryTeamType),
        Some("The teams that played"),
        resolve = _.value._2.teams
      ),
      Field(
        "boards",
        IntType,
        Some("The number boards that were played"),
        resolve = _.value._2.boards
      ),
      Field(
        "tables",
        IntType,
        Some("The number of tables that was used"),
        resolve = _.value._2.tables
      ),
      Field(
        "onlyresult",
        BooleanType,
        Some("true if this only contains the results"),
        resolve = _.value._2.onlyresult
      ),
      Field(
        "created",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value._2.created
      ),
      Field(
        "updated",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value._2.updated
      ),
      Field(
        "bestMatch",
        OptionType(BestMatchType),
        Some("The best match in the main store for this match"),
        resolve = ctx => DuplicateAction.getDuplicateBestMatch(ctx)
      )
    )
  )

  val MatchDuplicateType = ObjectType(
    "MatchDuplicate",
    "A duplicate match",
    fields[BridgeService, (Option[String], MatchDuplicate)](
      Field(
        "id",
        DuplicateIdType,
        Some("The id of the duplicate match"),
        resolve = _.value._2.id
      ),
      Field(
        "teams",
        ListType(DuplicateTeamType),
        Some("The teams that played"),
        resolve = _.value._2.teams
      ),
      Field(
        "boards",
        ListType(DuplicateBoardType),
        Some("The boards that were played"),
        resolve = _.value._2.boards
      ),
      Field(
        "boardset",
        StringType,
        Some("The boardset that was used"),
        resolve = _.value._2.boardset
      ),
      Field(
        "movement",
        StringType,
        Some("The movement that was used"),
        resolve = _.value._2.movement
      ),
      Field(
        "summary",
        DuplicateSummaryType,
        Some("The summary of the match"),
        resolve = ctx => (ctx.value._1, DuplicateSummary.create(ctx.value._2))
      ),
      Field(
        "created",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value._2.created
      ),
      Field(
        "updated",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value._2.updated
      )
    )
  )

  val BoardTeamResultsType = ObjectType(
    "BoardTeamResults",
    "The results on a board in a match",
    fields[BridgeService, (Option[String], BoardTeamResults)](
      Field("team", TeamIdType, Some(""), resolve = _.value._2.team),
      Field("points", FloatType, Some(""), resolve = _.value._2.points)
    )
  )

  val BoardResultsType = ObjectType(
    "BoardResults",
    "The results on a board in a match",
    fields[BridgeService, (Option[String], BoardResults)](
      Field("board", IntType, Some(""), resolve = _.value._2.board),
      Field(
        "points",
        ListType(BoardTeamResultsType),
        Some(""),
        resolve = ctx => ctx.value._2.points.map(r => (ctx.value._1, r))
      )
    )
  )

  val MatchDuplicateResultType = ObjectType(
    "MatchDuplicateResult",
    "A duplicate match",
    fields[BridgeService, (Option[String], MatchDuplicateResult)](
      Field(
        "id",
        DuplicateResultIdType,
        Some("The id of the duplicate match"),
        resolve = _.value._2.id
      ),
      Field(
        "results",
        ListType(ListType(DuplicateSummaryTeamType)),
        Some("The teams that played, and the results"),
        resolve = _.value._2.results
      ),
      Field(
        "boardresults",
        OptionType(ListType(BoardResultsType)),
        Some("The boards that were played"),
        resolve = ctx =>
          ctx.value._2.boardresults.map(r => r.map(rr => (ctx.value._1, rr)))
      ),
      Field(
        "comment",
        OptionType(StringType),
        Some("The comment"),
        resolve = _.value._2.comment
      ),
      Field(
        "notfinished",
        OptionType(BooleanType),
        Some("whether the match was finished"),
        resolve = _.value._2.notfinished
      ),
      Field(
        "summary",
        DuplicateSummaryType,
        Some("The summary of the match"),
        resolve = ctx => (ctx.value._1, DuplicateSummary.create(ctx.value._2))
      ),
      Field(
        "played",
        DateTimeType,
        Some("The time match was played"),
        resolve = _.value._2.played
      ),
      Field(
        "created",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value._2.created
      ),
      Field(
        "updated",
        DateTimeType,
        Some("The time the team was last updated"),
        resolve = _.value._2.updated
      )
    )
  )

  val CounterStatType = ObjectType(
    "CounterStat",
    "a counter",
    fields[BridgeService, CounterStat](
      Field(
        "id",
        IntType,
        Some("The id, number of tricks"),
        resolve = _.value.tricks
      ),
      Field(
        "tricks",
        IntType,
        Some("The number of tricks"),
        resolve = _.value.tricks
      ),
      Field("counter", IntType, Some("The counter"), resolve = _.value.counter)
    )
  )

  val DuplicatePlayerStatType = ObjectType(
    "DuplicatePlayerStat",
    "A duplicate player stat",
    fields[BridgeService, PlayerStat](
      Field(
        "id",
        StringType,
        Some("The id, player name, of the playerstat"),
        resolve = _.value.player
      ),
      Field(
        "player",
        StringType,
        Some("The player name"),
        resolve = _.value.player
      ),
      Field(
        "declarer",
        BooleanType,
        Some("true indicates these stats are for player on declaring team"),
        resolve = _.value.declarer
      ),
      Field(
        "contractType",
        StringType,
        Some("the contract type, partial, game, slam, grand slam"),
        resolve = _.value.contractType
      ),
      Field(
        "handsPlayed",
        IntType,
        Some("The number of hands played by player"),
        resolve = ctx => ctx.value.handsPlayed
      ),
      Field(
        "histogram",
        ListType(CounterStatType),
        Some("The comment"),
        resolve = ctx => ctx.value.histogram
      )
    )
  )

  val DuplicateContractStatType = ObjectType(
    "DuplicateContractStat",
    "A duplicate contract stat",
    fields[BridgeService, ContractStat](
      Field(
        "id",
        StringType,
        Some("The id, contract, of the playerstat"),
        resolve = _.value.contract
      ),
      Field(
        "contract",
        StringType,
        Some("The contract"),
        resolve = _.value.contract
      ),
      Field(
        "contractType",
        StringType,
        Some("the contract type, partial, game, slam, grand slam"),
        resolve = _.value.contractType
      ),
      Field(
        "handsPlayed",
        IntType,
        Some("The number of hands played by player"),
        resolve = ctx => ctx.value.handsPlayed
      ),
      Field(
        "histogram",
        ListType(CounterStatType),
        Some("The comment"),
        resolve = ctx => ctx.value.histogram
      )
    )
  )

  val DuplicatePlayerStatsType = ObjectType(
    "DuplicatePlayerStats",
    "A duplicate player stats",
    fields[BridgeService, PlayerStats](
      Field(
        "id",
        StringType,
        Some("The id of the duplicate match"),
        resolve = ctx => "playerStats"
      ),
      Field(
        "declarer",
        ListType(DuplicatePlayerStatType),
        Some("The stats for declarer"),
        resolve = _.value.declarer
      ),
      Field(
        "defender",
        ListType(DuplicatePlayerStatType),
        Some("The stats for defender"),
        resolve = _.value.defender
      ),
      Field(
        "min",
        IntType,
        Some("The minimum number of tricks made"),
        resolve = ctx => ctx.value.min
      ),
      Field(
        "max",
        IntType,
        Some("The maximum number of tricks made"),
        resolve = ctx => ctx.value.max
      )
    )
  )

  val PlayerComparisonStatType = ObjectType(
    "PlayerComparisonStat",
    "A duplicate player stats",
    fields[BridgeService, PlayerComparisonStat](
      Field(
        "id",
        StringType,
        Some("The id of the duplicate match"),
        resolve = ctx => ctx.value.player + "_" + ctx.value.stattype
      ),
      Field("player", StringType, Some("The player"), resolve = _.value.player),
      Field(
        "stattype",
        IntType,
        Some("The type of the auction"),
        resolve = _.value.stattype
      ),
      Field(
        "aggressivegood",
        IntType,
        Some("The number of good results the aggressive player got"),
        resolve = _.value.aggressivegood
      ),
      Field(
        "aggressivebad",
        IntType,
        Some("The number of bad results the aggressive player got"),
        resolve = _.value.aggressivebad
      ),
      Field(
        "aggressiveneutral",
        IntType,
        Some("The number of aggressive neutral results the player got"),
        resolve = _.value.aggressiveneutral
      ),
      Field(
        "passivegood",
        IntType,
        Some("The number of good results the passive player got"),
        resolve = _.value.passivegood
      ),
      Field(
        "passivebad",
        IntType,
        Some("The number of bad results the passive player got"),
        resolve = _.value.passivebad
      ),
      Field(
        "passiveneutral",
        IntType,
        Some("The number of passive neutral results the player got"),
        resolve = _.value.passiveneutral
      )
    )
  )

  val PlayerComparisonStatsType = ObjectType(
    "PlayerComparisonStats",
    "A duplicate player stats",
    fields[BridgeService, PlayerComparisonStats](
      Field(
        "id",
        StringType,
        Some("The id of the duplicate match"),
        resolve = ctx => "playerComparisonStats"
      ),
      Field(
        "data",
        ListType(PlayerComparisonStatType),
        Some("The stats for comparison"),
        resolve = _.value.data
      )
    )
  )

  val DuplicateContractStatsType = ObjectType(
    "DuplicateContractStats",
    "A duplicate contract stats",
    fields[BridgeService, ContractStats](
      Field(
        "id",
        StringType,
        Some("The id of the duplicate match"),
        resolve = ctx => "contractStats"
      ),
      Field(
        "data",
        ListType(DuplicateContractStatType),
        Some("The stats"),
        resolve = _.value.data
      ),
      Field(
        "min",
        IntType,
        Some("The minimum number of tricks made"),
        resolve = ctx => ctx.value.min
      ),
      Field(
        "max",
        IntType,
        Some("The maximum number of tricks made"),
        resolve = ctx => ctx.value.max
      )
    )
  )

  val PlayerOpponentStatType = ObjectType(
    "PlayerOpponentStatType",
    "player stats against an opponent in duplicate matches",
    fields[BridgeService, PlayerOpponentStat](
      Field(
        "id",
        OptionType(StringType),
        resolve = ctx => s"""${ctx.value.player}_${ctx.value.opponent}"""
      ),
      Field(
        "player",
        OptionType(StringType),
        resolve = _.value.player
      ),
      Field(
        "opponent",
        OptionType(StringType),
        resolve = _.value.opponent
      ),
      Field(
        "matchesPlayed",
        OptionType(IntType),
        resolve = _.value.matchesPlayed
      ),
      Field(
        "matchesBeat",
        OptionType(IntType),
        resolve = _.value.matchesBeat
      ),
      Field(
        "matchesTied",
        OptionType(IntType),
        resolve = _.value.matchesTied
      ),
      Field(
        "totalMP",
        OptionType(IntType),
        resolve = _.value.totalMP
      ),
      Field(
        "wonMP",
        OptionType(IntType),
        resolve = _.value.wonMP
      )
    )
  )

  val PlayerOpponentsStatType = ObjectType(
    "PlayerOpponentsStatType",
    "player stats against all opponent in duplicate matches",
    fields[BridgeService, PlayerOpponentsStat](
      Field(
        "id",
        OptionType(StringType),
        resolve = _.value.player
      ),
      Field(
        "player",
        OptionType(StringType),
        resolve = _.value.player
      ),
      Field(
        "opponents",
        OptionType(ListType(PlayerOpponentStatType)),
        resolve = _.value.opponents
      )
    )
  )

  val PlayersOpponentsStatsType = ObjectType(
    "PlayersOpponentsStatsType",
    "all player stats against all opponent in duplicate matches",
    fields[BridgeService, PlayersOpponentsStats](
      Field(
        "players",
        OptionType(ListType(PlayerOpponentsStatType)),
        resolve = _.value.players
      )
    )
  )

  val ArgDuplicateId = Argument(
    "id",
    DuplicateIdType,
    description = "The Id of the duplicate match"
  )

  val ArgDuplicateResultId = Argument(
    "id",
    DuplicateResultIdType,
    description = "The Id of the duplicate match"
  )

  val DuplicateStatsType = ObjectType(
    "DuplicateStatsType",
    "stats about duplicate matches",
    fields[BridgeService, BridgeService](
      Field(
        "playerStats",
        OptionType(DuplicatePlayerStatsType),
        resolve = ctx =>
          ctx.ctx.duplicates.readAll().map { rmap =>
            rmap match {
              case Right(map) =>
                PlayerStats.stats(map)
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error getting duplicates: ${statusCode} ${msg.msg}"
                )
            }
          }
      ),
      Field(
        "contractStats",
        OptionType(DuplicateContractStatsType),
        resolve = ctx =>
          ctx.ctx.duplicates.readAll().map { rmap =>
            rmap match {
              case Right(map) =>
                ContractStats.stats(map, false)
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error getting duplicates: ${statusCode} ${msg.msg}"
                )
            }
          }
      ),
      Field(
        "playerDoubledStats",
        OptionType(DuplicatePlayerStatsType),
        resolve = ctx =>
          ctx.ctx.duplicates.readAll().map { rmap =>
            rmap match {
              case Right(map) =>
                PlayerDoubledStats.stats(map)
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error getting duplicates: ${statusCode} ${msg.msg}"
                )
            }
          }
      ),
      Field(
        "comparisonStats",
        OptionType(PlayerComparisonStatsType),
        resolve = ctx =>
          ctx.ctx.duplicates.readAll().map { rmap =>
            rmap match {
              case Right(map) =>
                PlayerComparisonStats.stats(map)
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error getting duplicates: ${statusCode} ${msg.msg}"
                )
            }
          }
      ),
      Field(
        "playersOpponentsStats",
        OptionType(PlayersOpponentsStatsType),
        resolve = ctx =>
          ctx.ctx.duplicates.readAll().map { rmap =>
            rmap match {
              case Right(map) =>
                PlayersOpponentsStats.stats(map)
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error getting duplicates: ${statusCode} ${msg.msg}"
                )
            }
          }
      )
    )
  )

}

object DuplicateAction {
  import SchemaDuplicate._

  def getDuplicate(
      ctx: Context[BridgeService, BridgeService]
  ): Future[(Option[String], MatchDuplicate)] = {
    val id = ctx arg ArgDuplicateId
    ctx.value.duplicates.read(id).map { rmd =>
      rmd match {
        case Right(md) =>
          (if (ctx.ctx.id == ctx.value.id) None else Some(ctx.value.id), md)
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting match duplicate ${id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def getDuplicateResult(
      ctx: Context[BridgeService, BridgeService]
  ): Future[(Option[String], MatchDuplicateResult)] = {
    val id = ctx arg ArgDuplicateResultId
    ctx.value.duplicateresults.read(id).map { rmd =>
      rmd match {
        case Right(md) =>
          (if (ctx.ctx.id == ctx.value.id) None else Some(ctx.value.id), md)
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting match duplicate ${id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def getDuplicateBestMatch(
      ctx: Context[BridgeService, (Option[String], DuplicateSummary)]
  ): Future[Option[(Option[String], BestMatch)]] = {
    val mainStore = ctx.ctx
    val (importId, ds) = ctx.value
    val sourcestore =
      if (importId.isEmpty) Future.successful(Some(mainStore))
      else
        mainStore.importStore.get.get(importId.get).map { rbs =>
          rbs match {
            case Right(bs)   => Some(bs)
            case Left(error) => None
          }
        }
    if (ds.onlyresult) {
      val sourcemd = sourcestore.flatMap { ostore =>
        ostore match {
          case Some(store) =>
            store.duplicateresults.read(ds.id).map { rmd =>
              rmd match {
                case Right(md) =>
                  Some(md)
                case Left(err) =>
                  None
              }
            }
          case None =>
            Future.successful(None)
        }
      }
      sourcemd.flatMap { omd =>
        omd match {
          case Some(md) =>
            mainStore.duplicateresults.readAll().map { rlmd =>
              rlmd match {
                case Right(lmd) =>
                  val x =
                    lmd.values
                      .map { mmd =>
                        import DifferenceWrappers._
                        val diff = md.difference("", mmd)
                        log.fine(
                          s"Diff main(${mmd.id}) import(${importId},${md.id}): ${diff}"
                        )
                        BestMatch(mmd.id, diff)
                      }
                      .foldLeft(BestMatch.noMatch) { (ac, v) =>
                        if (ac.sameness < v.sameness) v
                        else ac
                      }
                  Some((importId, x))
                case Left(err) =>
                  None
              }
            }
          case None =>
            Future.successful(None)
        }
      }
    } else {
      val sourcemd = sourcestore.flatMap { ostore =>
        ostore match {
          case Some(store) =>
            store.duplicates.read(ds.id).map { rmd =>
              rmd match {
                case Right(md) =>
                  Some(md)
                case Left(err) =>
                  None
              }
            }
          case None =>
            Future.successful(None)
        }
      }
      sourcemd.flatMap { omd =>
        omd match {
          case Some(md) =>
            mainStore.duplicates.readAll().map { rlmd =>
              rlmd match {
                case Right(lmd) =>
                  val x =
                    lmd.values
                      .map { mmd =>
                        import DifferenceWrappers._
                        val diff = md.difference("", mmd)
                        log.fine(
                          s"Diff main(${mmd.id}) import(${importId},${md.id}): ${diff}"
                        )
                        BestMatch(mmd.id, diff)
                      }
                      .foldLeft(BestMatch.noMatch) { (ac, v) =>
                        if (ac.sameness < v.sameness) v
                        else ac
                      }
                  Some((importId, x))
                case Left(err) =>
                  None
              }
            }
          case None =>
            Future.successful(None)
        }
      }
    }
  }

  def getDuplicateFromRoot(
      ctx: Context[BridgeService, BridgeService]
  ): Future[MatchDuplicate] = {
    val id = ctx arg ArgDuplicateId
    ctx.ctx.duplicates.read(id).map { rmd =>
      rmd match {
        case Right(md) => md
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting match duplicate ${id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def sort(list: List[MatchDuplicate], sort: Option[Sort]) = {
    val l = sort
      .map { s =>
        s match {
          case SortCreated =>
            list.sortWith((l, r) => l.created < r.created)
          case SortCreatedDescending =>
            list.sortWith((l, r) => l.created > r.created)
          case SortId =>
            list.sortWith((l, r) => Id.idComparer(l.id, r.id) < 0)
        }
      }
      .getOrElse(list)
    log.info(s"""Returning list sorted with ${sort}: ${l
      .map(md => s"(${md.id},${md.created})")
      .mkString(",")}""")
    l
  }

  def sortSummary(list: List[DuplicateSummary], sort: Option[Sort]) = {
    val l = sort
      .map { s =>
        s match {
          case SortCreated =>
            list.sortWith((l, r) => l.created < r.created)
          case SortCreatedDescending =>
            list.sortWith((l, r) => l.created > r.created)
          case SortId =>
            list.sortWith((l, r) => Id.idComparer(l.id, r.id) < 0)
        }
      }
      .getOrElse(list)
    l
  }
}
