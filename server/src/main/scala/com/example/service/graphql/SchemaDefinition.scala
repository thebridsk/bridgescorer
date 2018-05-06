package com.example.service.graphql

import sangria.schema._
import com.example.backend.BridgeService
import com.example.data.MatchDuplicate
import com.example.data.MatchChicago
import com.example.data.MatchRubber
import com.example.service.graphql.Data.ImportBridgeService
import com.example.data.Team
import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.data.Hand
import scala.concurrent.Future
import com.example.backend.BridgeService

import scala.concurrent.ExecutionContext.Implicits.global
import com.example.data.Id
import sangria.validation.ValueCoercionViolation
import sangria.ast.ScalarValue
import sangria.ast
import com.example.data.SystemTime.Timestamp
import com.example.data.DuplicateSummary
import com.example.data.DuplicateSummaryEntry
import utils.logging.Logger
import com.example.data.BestMatch
import com.example.data.BestMatch
import com.example.data.Difference
import com.example.data.DifferenceWrappers
import com.example.data.MatchDuplicateResult
import com.example.data.BoardResults
import com.example.data.BoardTeamResults
import sangria.ast.AstLocation
import com.example.data.duplicate.stats.PlayerStat
import com.example.data.duplicate.stats.CounterStat
import com.example.data.duplicate.stats.ContractStat
import com.example.data.duplicate.stats.PlayerStats
import com.example.data.duplicate.stats.ContractStats
import com.example.data.duplicate.stats.PlayerDoubledStats

object SchemaDefinition {

  val log = Logger( SchemaDefinition.getClass.getName )

  def getPos( v: ast.Value ): Option[AstLocation] = v match {
    case ast.IntValue(value, comments, position) => position
    case ast.BigIntValue(value, comments, position ) => position
    case ast.FloatValue(value, comments, position ) => position
    case ast.BigDecimalValue(value, comments, position ) => position
    case ast.StringValue(value, block, blockRawValue, comments, position ) => position
    case ast.BooleanValue(value, comments, position ) => position
    case ast.EnumValue(value, comments, position ) => position
    case ast.ListValue(values, comments, position ) => position
    case ast.VariableValue(name, comments, position ) => position
    case ast.NullValue(comments, position ) => position
    case ast.ObjectValue(fields, comments, position ) => position
  }

  case class IdCoercionViolation( typename: String, extra: Option[String] = None )
      extends ValueCoercionViolation( s"""${typename} string expected ${extra.map( s=> ": "+s ).getOrElse("")}""" )

  def idScalarTypeFromString[T]( typename: String ) = ScalarType[T](typename,
    coerceOutput = (d, caps) =>  d.toString,

    coerceUserInput = {
      case s: String => Right( s.asInstanceOf[T] )
      case _ => Left(IdCoercionViolation(typename))
    },
    coerceInput = {
      case ast.StringValue(s, _, _, _, _) => Right( s.asInstanceOf[T] )
      case x => Left(IdCoercionViolation( typename, getPos(x).map( p => s"at line ${p.line} column ${p.column}, sourceId ${p.sourceId} index ${p.index}" ) ))
    })

  val TeamIdType = idScalarTypeFromString[Id.Team]("TeamId")
  val BoardIdType = idScalarTypeFromString[Id.DuplicateBoard]("BoardId")
  val DuplicateIdType = idScalarTypeFromString[Id.MatchDuplicate]("DuplicateId")
  val DuplicateResultIdType = idScalarTypeFromString[Id.MatchDuplicateResult]("DuplicateResultId")
  val ChicagoIdType = idScalarTypeFromString[Id.MatchChicago]("ChicagoId")
  val RubberIdType = idScalarTypeFromString[String]("RubberId")
  val ImportIdType = idScalarTypeFromString[String]("ImportId")

  val DateTimeType = ScalarType[Timestamp]( "Timestamp",
    description = Some( "Timestamp, milliseconds since 1/1/1970."),
    coerceOutput = FloatType.coerceOutput(_,_),
    coerceUserInput = FloatType.coerceUserInput(_).map( v => v.asInstanceOf[Timestamp]),
    coerceInput = FloatType.coerceInput(_).map( v => v.asInstanceOf[Timestamp])
  )

  val DuplicateTeamType = ObjectType(
      "DuplicateTeam",
      "A duplicate team",
      fields[BridgeService,Team](
          Field("id", TeamIdType,
              Some("The id of the team"),
              resolve = _.value.id
          ),
          Field("player1", StringType,
              Some("The name of player 1"),
              resolve = _.value.player1
          ),
          Field("player2", StringType,
              Some("The name of player 2"),
              resolve = _.value.player2
          ),
          Field("created", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value.updated
          ),
          Field("updated", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value.updated
          )
      )
  )

  val HandType = ObjectType(
      "Hand",
      "Result of a bridge hand",
      fields[BridgeService,Hand](
          Field("id", StringType,
              Some("The id of the hand"),
              resolve = _.value.id
          ),
          Field("contractTricks", IntType,
              Some("The number of tricks in the contract."),
              resolve = _.value.contractTricks
          ),
          Field("contractSuit", StringType,
              Some("The suit in the contract."),
              resolve = _.value.contractSuit
          ),
          Field("contractDoubled", StringType,
              Some("The doubling of the contract."),
              resolve = _.value.contractSuit
          ),
          Field("declarer", StringType,
              Some("The declarer of the contract."),
              resolve = _.value.declarer
          ),
          Field("nsVul", BooleanType,
              Some("The vulnerability of NS for the contract."),
              resolve = _.value.nsVul
          ),
          Field("ewVul", BooleanType,
              Some("The vulnerability of EW for the contract."),
              resolve = _.value.ewVul
          ),
          Field("madeContract", BooleanType,
              Some("Whether the contract was made or not."),
              resolve = _.value.madeContract
          ),
          Field("tricks", IntType,
              Some("The number of tricks made or down in the contract."),
              resolve = _.value.tricks
          ),
          Field("created", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value.updated
          ),
          Field("updated", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value.updated
          )
      )
  )

  val DuplicateHandType = ObjectType(
      "DuplicateHand",
      "A duplicate hand",
      fields[BridgeService,DuplicateHand](
          Field("id", TeamIdType,
              Some("The id of the NS team"),
              resolve = _.value.id
          ),
          Field("played",
              ListType( HandType ),
              Some("The played hand"),
              resolve = _.value.played
          ),
          Field("table", StringType,
              Some("The table the hand was played on"),
              resolve = _.value.table
          ),
          Field("round", IntType,
              Some("The round the hand was played in"),
              resolve = _.value.round
          ),
          Field("board", StringType,
              Some("The board"),
              resolve = _.value.board
          ),
          Field("nsTeam", TeamIdType,
              Some("The NS team that plays the hand"),
              resolve = _.value.nsTeam
          ),
          Field("nIsPlayer1", BooleanType,
              Some("The north player is player 1 of the team"),
              resolve = _.value.nIsPlayer1
          ),
          Field("ewTeam", TeamIdType,
              Some("The EW team that plays the hand"),
              resolve = _.value.nsTeam
          ),
          Field("eIsPlayer1", BooleanType,
              Some("The east player is player 1 of the team"),
              resolve = _.value.eIsPlayer1
          ),
          Field("created", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value.updated
          ),
          Field("updated", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value.updated
          )
      )
  )

  val ArgHandId = Argument( "id",
                         TeamIdType,
                         description = "The Id of the hand.  This the team id of the NS team." )

  val DuplicateBoardType = ObjectType(
      "DuplicateBoard",
      "A duplicate board",
      fields[BridgeService,Board](
          Field("id", DuplicateIdType,
              Some("The id of the board"),
              resolve = _.value.id
          ),
          Field("nsVul", BooleanType,
              Some("The vulnerability of NS for the contract."),
              resolve = _.value.nsVul
          ),
          Field("ewVul", BooleanType,
              Some("The vulnerability of EW for the contract."),
              resolve = _.value.ewVul
          ),
          Field("dealer", StringType,
              Some("The dealer"),
              resolve = _.value.dealer
          ),
          Field("hands",
              ListType( DuplicateHandType ),
              Some("The played hand"),
              resolve = _.value.hands
          ),
          Field("hand",
              OptionType( DuplicateHandType ),
              Some("The played hand"),
              arguments = ArgHandId::Nil,
              resolve = ctx => {
                val id = ctx arg ArgHandId
                ctx.value.getHand( id )
              }
          ),
          Field("created", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value.updated
          ),
          Field("updated", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value.updated
          )
      )
  )

  val ArgTeamId = Argument( "id",
                         TeamIdType,
                         description = "The Id of the team." )

  val ArgBoardId = Argument( "id",
                          BoardIdType,
                          description = "The Id of the board." )

  val DuplicateSummaryTeamType = ObjectType(
      "DuplicateSummaryTeam",
      "A team result in a DuplicateSummary",
      fields[BridgeService,DuplicateSummaryEntry](
          Field("id", TeamIdType,
              Some("The id of the duplicate match"),
              resolve = _.value.team.id
          ),
          Field("team", DuplicateTeamType,
              Some("The team"),
              resolve = _.value.team
          ),
          Field("result", OptionType(FloatType),
              Some("The points the team scored"),
              resolve = _.value.result
          ),
          Field("place", OptionType(IntType),
              Some("The place the team finished in"),
              resolve = _.value.place
          ),
          Field("resultIMP", OptionType(FloatType),
              Some("The IMPs the team scored"),
              resolve = _.value.resultImp
          ),
          Field("placeIMP", OptionType(IntType),
              Some("The place the team finished in using IMPs"),
              resolve = _.value.placeImp
          )
      )
  )

  val BestMatchType = ObjectType(
      "BestMatch",
      "Identifies the best match",
      fields[BridgeService,(Option[String],BestMatch)](
          Field("id", OptionType(DuplicateIdType),
              Some("The id of the best duplicate match from the main store"),
              resolve = _.value._2.id
          ),
          Field("sameness", FloatType,
              Some("A percentage of similarity."),
              resolve = _.value._2.sameness
          ),
          Field("differences", OptionType(ListType(StringType)),
              Some("The fields that are different"),
              resolve = _.value._2.differences
          ),
      )
  )

  val DuplicateSummaryType = ObjectType(
      "DuplicateSummary",
      "A duplicate match",
      fields[BridgeService,(Option[String],DuplicateSummary)](
          Field("id", DuplicateIdType,
              Some("The id of the duplicate match"),
              resolve = _.value._2.id
          ),
          Field("finished", BooleanType,
              Some("true if the match is finished"),
              resolve = _.value._2.finished
          ),
          Field("teams",
              ListType( DuplicateSummaryTeamType ),
              Some("The teams that played"),
              resolve = _.value._2.teams
          ),
          Field("boards",
              IntType,
              Some("The number boards that were played"),
              resolve = _.value._2.boards
          ),
          Field("tables", IntType,
              Some("The number of tables that was used"),
              resolve = _.value._2.tables
          ),
          Field("onlyresult", BooleanType,
              Some("true if this only contains the results"),
              resolve = _.value._2.onlyresult
          ),
          Field("created", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value._2.updated
          ),
          Field("updated", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value._2.updated
          ),
          Field("bestMatch",
              OptionType(BestMatchType),
              Some("The best match in the main store for this match"),
              resolve = ctx => Action.getDuplicateBestMatch(ctx)
          )
      )
  )

  val MatchDuplicateType = ObjectType(
      "MatchDuplicate",
      "A duplicate match",
      fields[BridgeService,(Option[String],MatchDuplicate)](
          Field("id", DuplicateIdType,
              Some("The id of the duplicate match"),
              resolve = _.value._2.id
          ),
          Field("teams",
              ListType( DuplicateTeamType ),
              Some("The teams that played"),
              resolve = _.value._2.teams
          ),
          Field("boards",
              ListType( DuplicateBoardType ),
              Some("The boards that were played"),
              resolve = _.value._2.boards
          ),
          Field("boardset", StringType,
              Some("The boardset that was used"),
              resolve = _.value._2.boardset
          ),
          Field("movement", StringType,
              Some("The movement that was used"),
              resolve = _.value._2.movement
          ),
          Field("summary", DuplicateSummaryType,
              Some("The summary of the match"),
              resolve = ctx => (ctx.value._1,DuplicateSummary.create(ctx.value._2))
          ),
          Field("created", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value._2.updated
          ),
          Field("updated", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value._2.updated
          )
      )
  )

  val BoardTeamResultsType = ObjectType(
      "BoardTeamResults",
      "The results on a board in a match",
      fields[BridgeService,(Option[String],BoardTeamResults)](
          Field("team", TeamIdType,
              Some(""),
              resolve = _.value._2.team
          ),
          Field("points", FloatType,
              Some(""),
              resolve = _.value._2.points
          )
      )
  )

  val BoardResultsType = ObjectType(
      "BoardResults",
      "The results on a board in a match",
      fields[BridgeService,(Option[String],BoardResults)](
          Field("board", IntType,
              Some(""),
              resolve = _.value._2.board
          ),
          Field("points", ListType(BoardTeamResultsType),
              Some(""),
              resolve = ctx => ctx.value._2.points.map( r => (ctx.value._1,r))
          )
      )
  )

  val MatchDuplicateResultType = ObjectType(
      "MatchDuplicateResult",
      "A duplicate match",
      fields[BridgeService,(Option[String],MatchDuplicateResult)](
          Field("id", DuplicateResultIdType,
              Some("The id of the duplicate match"),
              resolve = _.value._2.id
          ),
          Field("results",
              ListType( ListType( DuplicateSummaryTeamType ) ),
              Some("The teams that played, and the results"),
              resolve = _.value._2.results
          ),
          Field("boardresults",
              OptionType( ListType( BoardResultsType )),
              Some("The boards that were played"),
              resolve = ctx => ctx.value._2.boardresults.map(r => r.map( rr=> (ctx.value._1,rr)))
          ),
          Field("comment", OptionType(StringType),
              Some("The comment"),
              resolve = _.value._2.comment
          ),
          Field("notfinished", OptionType(BooleanType),
              Some("whether the match was finished"),
              resolve = _.value._2.notfinished
          ),
          Field("summary", DuplicateSummaryType,
              Some("The summary of the match"),
              resolve = ctx => (ctx.value._1,DuplicateSummary.create(ctx.value._2))
          ),
          Field("played", DateTimeType,
              Some("The time match was played"),
              resolve = _.value._2.played
          ),
          Field("created", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value._2.updated
          ),
          Field("updated", DateTimeType,
              Some("The time the team was last updated"),
              resolve = _.value._2.updated
          )
      )
  )

  val CounterStatType = ObjectType(
      "CounterStat",
      "a counter",
      fields[BridgeService,CounterStat](
          Field("id", IntType,
              Some("The id, number of tricks"),
              resolve = _.value.tricks
          ),
          Field("tricks", IntType,
              Some("The number of tricks"),
              resolve = _.value.tricks
          ),
          Field("counter", IntType,
              Some("The counter"),
              resolve = _.value.counter
          )
      )
  )

  val DuplicatePlayerStatType = ObjectType(
      "DuplicatePlayerStat",
      "A duplicate player stat",
      fields[BridgeService, PlayerStat](
          Field("id",
              StringType,
              Some("The id, player name, of the playerstat"),
              resolve = _.value.player
          ),
          Field("player",
              StringType,
              Some("The player name"),
              resolve = _.value.player
          ),
          Field("declarer",
              BooleanType,
              Some("true indicates these stats are for player on declaring team"),
              resolve = _.value.declarer
          ),
          Field("contractType",
              StringType,
              Some("the contract type, partial, game, slam, grand slam"),
              resolve = _.value.contractType
          ),
          Field("handsPlayed",
              IntType,
              Some("The number of hands played by player"),
              resolve = ctx => ctx.value.handsPlayed
          ),
          Field("histogram",
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
          Field("id",
              StringType,
              Some("The id, contract, of the playerstat"),
              resolve = _.value.contract
          ),
          Field("contract",
              StringType,
              Some("The contract"),
              resolve = _.value.contract
          ),
          Field("contractType",
              StringType,
              Some("the contract type, partial, game, slam, grand slam"),
              resolve = _.value.contractType
          ),
          Field("handsPlayed",
              IntType,
              Some("The number of hands played by player"),
              resolve = ctx => ctx.value.handsPlayed
          ),
          Field("histogram",
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
          Field("id",
              StringType,
              Some("The id of the duplicate match"),
              resolve = ctx => "playerStats"
          ),
          Field("declarer",
              ListType( DuplicatePlayerStatType ),
              Some("The stats for declarer"),
              resolve = _.value.declarer
          ),
          Field("defender",
              ListType( DuplicatePlayerStatType ),
              Some("The stats for defender"),
              resolve = _.value.defender
          ),
          Field("min",
              IntType,
              Some("The minimum number of tricks made"),
              resolve = ctx => ctx.value.min
          ),
          Field("max",
              IntType,
              Some("The maximum number of tricks made"),
              resolve = ctx => ctx.value.max
          )
      )
  )

  val DuplicateContractStatsType = ObjectType(
      "DuplicateContractStats",
      "A duplicate contract stats",
      fields[BridgeService, ContractStats](
          Field("id",
              StringType,
              Some("The id of the duplicate match"),
              resolve = ctx => "contractStats"
          ),
          Field("data",
              ListType( DuplicateContractStatType ),
              Some("The stats"),
              resolve = _.value.data
          ),
          Field("min",
              IntType,
              Some("The minimum number of tricks made"),
              resolve = ctx => ctx.value.min
          ),
          Field("max",
              IntType,
              Some("The maximum number of tricks made"),
              resolve = ctx => ctx.value.max
          )
      )
  )


  val ArgDuplicateId = Argument("id",
                             DuplicateIdType,
                             description = "The Id of the duplicate match" )

  val ArgDuplicateResultId = Argument("id",
                             DuplicateResultIdType,
                             description = "The Id of the duplicate match" )

  val ArgImportId = Argument("id",
                          ImportIdType,
                          description = "The Id of the import" )

  trait Sort
  case object SortCreated extends Sort
  case object SortCreatedDescending extends Sort
  case object SortId extends Sort


  val SortEnum = EnumType[Sort](
    "Sort",
    Some("how the result should be sorted"),
    List(
      EnumValue("created",
        value = SortCreated,
        description = Some("Sort by created field, ascending.")
      ),
      EnumValue("reversecreated",
        value = SortCreatedDescending,
        description = Some("Sort by created field, descending.")
      ),
      EnumValue("id",
        value = SortId,
        description = Some("Sort by Id")
      )
    )
  )

  val ArgSort = Argument("sort",
                          OptionInputType( SortEnum ),
                          description = "If specified, identifies the sort order of the values in list." )

  val BridgeServiceType = ObjectType(
      "BridgeService",
      fields[BridgeService,BridgeService](
          Field("id", ImportIdType,
              Some("The id of the bridge service"),
              resolve = _.value.id),
          Field("date", DateTimeType,
              Some("The date for the store."),
              resolve = _.value.getDate),
          Field("duplicate",
                OptionType(MatchDuplicateType),
                arguments = ArgDuplicateId::Nil,
                resolve = Action.getDuplicate
          ),
          Field("duplicates",
              ListType( MatchDuplicateType ),
              resolve = ctx => ctx.value.duplicates.readAll().map{ rall =>
                rall match {
                  case Right(all) => all.values.toList.map { md => (if (ctx.ctx.id==ctx.value.id) None else Some(ctx.value.id),md) }
                  case Left((statusCode,msg)) => throw new Exception( s"Error getting MatchDuplicates: ${statusCode} ${msg.msg}" )
                }
              }
          ),
          Field("duplicatesummaries",
                ListType( DuplicateSummaryType ),
                arguments = ArgSort::Nil,
                resolve = ctx => ctx.value.getDuplicateSummaries().map { rmap => rmap match {
                            case Right(list) =>
                              val argsort = ctx.arg( ArgSort )
                              Action.sortSummary( list, argsort ).map { md => (if (ctx.ctx.id==ctx.value.id) None else Some(ctx.value.id),md) }
                            case Left((statusCode,msg)) =>
                              throw new Exception(s"Error getting duplicate summaries: ${statusCode} ${msg.msg}")
                          }
                }
          ),
          Field("duplicateIds",
              ListType( StringType ),
              resolve = _.value.duplicates.readAll().map{ rall =>
                rall match {
                  case Right(all) => all.keys.toList
                  case Left((statusCode,msg)) => throw new Exception( s"Error getting MatchDuplicates: ${statusCode} ${msg.msg}" )
                }
              }
          )
      )
  )

  val DuplicateStatsType = ObjectType(
      "DuplicateStatsType",
      "stats about duplicate matches",
      fields[BridgeService,BridgeService](
          Field(
              "playerStats",
              DuplicatePlayerStatsType,
              resolve = ctx => ctx.ctx.duplicates.readAll().map { rmap => rmap match {
                          case Right(map) =>
                            PlayerStats.stats(map)
                          case Left((statusCode,msg)) =>
                            throw new Exception(s"Error getting duplicates: ${statusCode} ${msg.msg}")
                        }
              }
          ),
          Field(
              "contractStats",
              DuplicateContractStatsType,
              resolve = ctx => ctx.ctx.duplicates.readAll().map { rmap => rmap match {
                          case Right(map) =>
                            ContractStats.stats(map, false)
                          case Left((statusCode,msg)) =>
                            throw new Exception(s"Error getting duplicates: ${statusCode} ${msg.msg}")
                        }
              }
          ),
          Field(
              "playerDoubledStats",
              DuplicatePlayerStatsType,
              resolve = ctx => ctx.ctx.duplicates.readAll().map { rmap => rmap match {
                          case Right(map) =>
                            PlayerDoubledStats.stats(map)
                          case Left((statusCode,msg)) =>
                            throw new Exception(s"Error getting duplicates: ${statusCode} ${msg.msg}")
                        }
              }
          )
      )
  )

  val QueryType = ObjectType(
      "Query",
      fields[BridgeService,Unit](
          Field("importIds",
                ListType( ImportIdType ),
                resolve = _.ctx.importStore match {
                            case Some(is) =>
                              is.getAllIds().map( rlist => rlist match {
                                case Right(list) =>
                                  list
                                case Left((statusCode,msg)) =>
                                  throw new Exception(s"Error getting import store ids: ${statusCode} ${msg.msg}")
                              })
                            case None =>
                              throw new Exception("Did not find the import store")
                          }
          ),
          Field("imports",
                ListType( BridgeServiceType ),
                resolve = Action.getAllImportFromRoot
          ),
          Field("import",
                OptionType(BridgeServiceType),
                arguments = ArgImportId::Nil,
                resolve = Action.getImportFromRoot
          ),
          Field("duplicateIds",
                ListType( DuplicateIdType ),
                resolve = _.ctx.duplicates.readAll().map { rmap => rmap match {
                            case Right(map) =>
                              map.keys.toList
                            case Left((statusCode,msg)) =>
                              throw new Exception(s"Error getting duplicate ids: ${statusCode} ${msg.msg}")
                          }
                }
          ),
          Field("duplicateResultIds",
                ListType( DuplicateResultIdType ),
                resolve = _.ctx.duplicateresults.readAll().map { rmap => rmap match {
                            case Right(map) =>
                              map.keys.toList
                            case Left((statusCode,msg)) =>
                              throw new Exception(s"Error getting duplicate ids: ${statusCode} ${msg.msg}")
                          }
                }
          ),
          Field("chicagoIds",
                ListType( ChicagoIdType ),
                resolve = _.ctx.chicagos.readAll().map { rmap => rmap match {
                            case Right(map) =>
                              map.keys.toList
                            case Left((statusCode,msg)) =>
                              throw new Exception(s"Error getting chicago ids: ${statusCode} ${msg.msg}")
                          }
                }
          ),
          Field("rubberIds",
                ListType( RubberIdType ),
                resolve = _.ctx.rubbers.readAll().map { rmap => rmap match {
                            case Right(map) =>
                              map.keys.toList
                            case Left((statusCode,msg)) =>
                              throw new Exception(s"Error getting rubber ids: ${statusCode} ${msg.msg}")
                          }
                }
          ),
          Field("duplicates",
                ListType( MatchDuplicateType ),
                arguments = ArgSort::Nil,
                resolve = ctx => ctx.ctx.duplicates.readAll().map { rmap => rmap match {
                            case Right(map) =>
                              val list = map.values.toList
                              val argsort = ctx.arg( ArgSort )
                              Action.sort(list,argsort).map { md => (None,md) }
                            case Left((statusCode,msg)) =>
                              throw new Exception(s"Error getting duplicates: ${statusCode} ${msg.msg}")
                          }
                }
          ),
          Field("duplicate",
                OptionType(MatchDuplicateType),
                arguments = ArgDuplicateId::Nil,
                resolve = ctx => Action.getDuplicateFromRoot(ctx).map { md => (None,md) }
          ),
          Field("duplicatesummaries",
                ListType( DuplicateSummaryType ),
                arguments = ArgSort::Nil,
                resolve = ctx => ctx.ctx.getDuplicateSummaries().map { rmap => rmap match {
                            case Right(list) =>
                              val argsort = ctx.arg( ArgSort )
                              Action.sortSummary( list, argsort ).map { md => (None,md) }
                            case Left((statusCode,msg)) =>
                              throw new Exception(s"Error getting duplicate summaries: ${statusCode} ${msg.msg}")
                          }
                }
          ),
          Field(
              "duplicatestats",
              DuplicateStatsType,
              resolve = ctx => ctx.ctx
          )

      )
  )

  val MutationImportType = ObjectType(
      "MutationImport",
      fields[BridgeService,BridgeService](
          Field("id", ImportIdType,
              Some("The id of the import store"),
              resolve = _.value.id),
          Field("importduplicate",
                MatchDuplicateType,
                description = Some("Import a duplicate match."),
                arguments = ArgDuplicateId::Nil,
                resolve = ctx => Action.importDuplicate(ctx).map { md => (None,md) }
          ),
          Field("importduplicateresult",
                MatchDuplicateResultType,
                description = Some("Import a duplicate result."),
                arguments = ArgDuplicateResultId::Nil,
                resolve = ctx => Action.importDuplicateResult(ctx).map { md => (None,md) }
          ),
          Field("delete",
                BooleanType,
                description = Some("Delete the import store"),
                resolve = Action.deleteImportStore
          )
      )
  )

  val MutationType = ObjectType("Mutation", fields[BridgeService,Unit](
    Field("import",
          OptionType(MutationImportType),
          description = Some("Selecting the import store from which imports are done.  returns null if not found."),
          arguments = ArgImportId::Nil,
          resolve = Action.getImportFromRoot
    )
  ))

  val BridgeScorerSchema = Schema( QueryType, Some(MutationType) )
}

object Action {
  import SchemaDefinition._

  def getDuplicate( ctx: Context[BridgeService,BridgeService]): Future[(Option[String],MatchDuplicate)] = {
    val id = ctx arg ArgDuplicateId
    ctx.value.duplicates.read(id).map { rmd =>
      rmd match {
        case Right(md) => (if (ctx.ctx.id==ctx.value.id) None else Some(ctx.value.id),md)
        case Left((statusCode,msg)) =>
          throw new Exception(s"Error getting match duplicate ${id}: ${statusCode} ${msg.msg}")
      }
    }
  }

  def getDuplicateBestMatch( ctx: Context[BridgeService,(Option[String],DuplicateSummary)]): Future[Option[(Option[String],BestMatch)]] = {
    val mainStore = ctx.ctx
    val (importId,ds) = ctx.value
    val sourcestore = if (importId.isEmpty) Future.successful(Some(mainStore))
                      else mainStore.importStore.get.get(importId.get).map { rbs =>
                        rbs match {
                          case Right(bs) => Some(bs)
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
                  lmd.values.map { mmd =>
                    import DifferenceWrappers._
                    val diff = md.difference("", mmd)
                    log.fine(s"Diff main(${mmd.id}) import(${importId},${md.id}): ${diff}")
                    BestMatch( mmd.id, diff )
                  }.foldLeft(BestMatch.noMatch) { (ac,v) =>
                    if (ac.sameness < v.sameness) v
                    else ac
                  }
                  Some((importId,x))
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
                  lmd.values.map { mmd =>
                    import DifferenceWrappers._
                    val diff = md.difference("", mmd)
                    log.fine(s"Diff main(${mmd.id}) import(${importId},${md.id}): ${diff}")
                    BestMatch( mmd.id, diff )
                  }.foldLeft(BestMatch.noMatch) { (ac,v) =>
                    if (ac.sameness < v.sameness) v
                    else ac
                  }
                  Some((importId,x))
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

  def getDuplicateFromRoot( ctx: Context[BridgeService,Unit]): Future[MatchDuplicate] = {
    val id = ctx arg ArgDuplicateId
    ctx.ctx.duplicates.read(id).map { rmd =>
      rmd match {
        case Right(md) => md
        case Left((statusCode,msg)) =>
          throw new Exception(s"Error getting match duplicate ${id}: ${statusCode} ${msg.msg}")
      }
    }
  }

  def getAllImportFromRoot( ctx: Context[BridgeService,Unit]): Future[List[BridgeService]] = {
    ctx.ctx.importStore match {
      case Some(is) =>
        is.getAllIds().flatMap { rlids =>
          rlids match {
            case Right(lids) =>
              val fbss = lids.map { id =>
                is.get(id)
              }
              Future.foldLeft(fbss)(List[BridgeService]()) { (ac,v) =>
                v match {
                  case Right(bs) => bs::ac
                  case Left(err) => ac
                }
              }
            case Left((statusCode,msg)) =>
              throw new Exception(s"Error getting all import IDs: ${statusCode} ${msg.msg}")
          }
        }
      case None =>
        throw new Exception(s"Did not find the import store")
    }
  }

  def getImportFromRoot( ctx: Context[BridgeService,Unit]): Future[BridgeService] = {
    val id = ctx arg ArgImportId
    ctx.ctx.importStore match {
      case Some(is) =>
        is.get(id).map( rbs => rbs match {
          case Right(bs) =>
            bs
          case Left((statusCode,msg)) =>
            log.fine(s"Error getting import store ${id}: ${statusCode} ${msg.msg}")
//            throw new Exception(s"Error getting import store ${id}: ${statusCode} ${msg.msg}")
            null
        })
      case None =>
        throw new Exception(s"Did not find the import store ${id}")
    }
  }

  def importDuplicate( ctx: Context[BridgeService,BridgeService]): Future[MatchDuplicate] = {
    val dupId = ctx arg ArgDuplicateId
    val bs = ctx.value
    bs.duplicates.read(dupId).flatMap { rdup => rdup match {
      case Right(dup) =>
        ctx.ctx.duplicates.importChild(dup).map { rc => rc match {
          case Right(cdup) =>
            cdup
          case Left((statusCode,msg)) =>
            throw new Exception(s"Error importing into store: ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}")
        }}
      case Left((statusCode,msg)) =>
        throw new Exception(s"Error getting ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}")
    }}
  }

  def importDuplicateResult( ctx: Context[BridgeService,BridgeService]): Future[MatchDuplicateResult] = {
    val dupId = ctx arg ArgDuplicateId
    val bs = ctx.value
    bs.duplicateresults.read(dupId).flatMap { rdup => rdup match {
      case Right(dup) =>
        ctx.ctx.duplicateresults.importChild(dup).map { rc => rc match {
          case Right(cdup) =>
            cdup
          case Left((statusCode,msg)) =>
            throw new Exception(s"Error importing into store: ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}")
        }}
      case Left((statusCode,msg)) =>
        throw new Exception(s"Error getting ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}")
    }}
  }

  def deleteImportStore( ctx: Context[BridgeService,BridgeService]): Future[Boolean] = {
    val todelete = ctx.value
    ctx.ctx.importStore match {
      case Some(is) =>
        is.delete(todelete.id).map { rd =>
          rd match {
            case Right(x) =>
              log.warning(s"Deleted import store ${todelete.id}")
              true
            case Left(error) =>
              log.warning(s"Error deleting import store ${todelete.id}: ${error}")
              false
          }
        }.recover {
          case x: Exception =>
            log.warning(s"Error deleting import store ${todelete.id}", x)
            false
        }
      case None =>
        log.fine("Did not find import store")
        Future.successful(false)
    }
  }

  def sort( list: List[MatchDuplicate], sort: Option[Sort] ) = {
    val l = sort.map { s =>
      s match {
        case SortCreated =>
          list.sortWith((l,r)=>l.created<r.created)
        case SortCreatedDescending =>
          list.sortWith((l,r)=>l.created>r.created)
        case SortId =>
          list.sortWith((l,r)=>Id.idComparer(l.id, r.id) < 0)
      }
    }.getOrElse(list)
    log.info( s"""Returning list sorted with ${sort}: ${l.map( md=> s"(${md.id},${md.created})").mkString(",")}""" )
    l
  }

  def sortSummary( list: List[DuplicateSummary], sort: Option[Sort] ) = {
    val l = sort.map { s => s match {
        case SortCreated =>
          list.sortWith((l,r)=>l.created<r.created)
        case SortCreatedDescending =>
          list.sortWith((l,r)=>l.created>r.created)
        case SortId =>
          list.sortWith((l,r)=>Id.idComparer(l.id, r.id) < 0)
      }
    }.getOrElse(list)
    l
  }
}
