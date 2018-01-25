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
import org.parboiled2.Position
import sangria.ast.ScalarValue
import sangria.ast
import com.example.data.SystemTime.Timestamp

object SchemaDefinition {

  def getPos( v: ast.Value ): Option[Position] = v match {
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
      case x => Left(IdCoercionViolation( typename, getPos(x).map( p => s"at line ${p.line} column ${p.column}" ) ))
    })

  val TeamIdType = idScalarTypeFromString[Id.Team]("TeamId")
  val BoardIdType = idScalarTypeFromString[Id.DuplicateBoard]("BoardId")
  val DuplicateIdType = idScalarTypeFromString[Id.MatchDuplicate]("DuplicateId")
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
      fields[Unit,Team](
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
      fields[Unit,Hand](
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
      fields[Unit,DuplicateHand](
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
      fields[Unit,Board](
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

  val MatchDuplicateType = ObjectType(
      "MatchDuplicate",
      "A duplicate match",
      fields[Unit,MatchDuplicate](
          Field("id", DuplicateIdType,
              Some("The id of the duplicate match"),
              resolve = _.value.id
          ),
          Field("teams",
              ListType( DuplicateTeamType ),
              Some("The teams that played"),
              resolve = _.value.teams
          ),
          Field("boards",
              ListType( DuplicateBoardType ),
              Some("The boards that were played"),
              resolve = _.value.boards
          ),
          Field("boardset", StringType,
              Some("The boardset that was used"),
              resolve = _.value.boardset
          ),
          Field("movement", StringType,
              Some("The movement that was used"),
              resolve = _.value.movement
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


  val ArgDuplicateId = Argument("id",
                             DuplicateIdType,
                             description = "The Id of the duplicate match" )

  val ArgImportId = Argument("id",
                          ImportIdType,
                          description = "The Id of the import" )

  val BridgeServiceType = ObjectType(
      "BridgeService",
      fields[Unit,BridgeService](
          Field("id", ImportIdType,
              Some("The id of the bridge service"),
              resolve = _.value.id),
          Field("duplicate",
                MatchDuplicateType,
                arguments = ArgDuplicateId::Nil,
                resolve = Action.getDuplicate
          ),
          Field("duplicates",
              ListType( MatchDuplicateType ),
              resolve = _.value.duplicates.readAll().map{ rall =>
                rall match {
                  case Right(all) => all.values.toList
                  case Left((statusCode,msg)) => throw new Exception( s"Error getting MatchDuplicates: ${statusCode} ${msg.msg}" )
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

  val Query = ObjectType(
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
          Field("import",
                BridgeServiceType,
                arguments = ArgImportId::Nil,
                resolve = Action.getImportFromRoot
          ),
          Field("duplicateIds",
                ListType( StringType ),
                resolve = _.ctx.duplicates.readAll().map { rmap => rmap match {
                            case Right(map) =>
                              map.keys.toList
                            case Left((statusCode,msg)) =>
                              throw new Exception(s"Error getting duplicate ids: ${statusCode} ${msg.msg}")
                          }
                }
          ),
          Field("duplicates",
                ListType( MatchDuplicateType ),
                resolve = _.ctx.duplicates.readAll().map { rmap => rmap match {
                            case Right(map) =>
                              map.values.toList
                            case Left((statusCode,msg)) =>
                              throw new Exception(s"Error getting duplicates: ${statusCode} ${msg.msg}")
                          }
                }
          ),
          Field("duplicate",
                MatchDuplicateType,
                arguments = ArgDuplicateId::Nil,
                resolve = Action.getDuplicateFromRoot
          )

      )
  )

  val BridgeScorerSchema = Schema( Query )
}

object Action {
  import SchemaDefinition._

  def getDuplicate( ctx: Context[Unit,BridgeService]): Future[MatchDuplicate] = {
    val id = ctx arg ArgDuplicateId
    ctx.value.duplicates.read(id).map { rmd =>
      rmd match {
        case Right(md) => md
        case Left((statusCode,msg)) =>
          throw new Exception(s"Error getting match duplicate ${id}: ${statusCode} ${msg.msg}")
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

  def getImportFromRoot( ctx: Context[BridgeService,Unit]): Future[BridgeService] = {
    val id = ctx arg ArgImportId
    ctx.ctx.importStore match {
      case Some(is) =>
        is.get(id).map( rbs => rbs match {
          case Right(bs) =>
            bs
          case Left((statusCode,msg)) =>
            throw new Exception(s"Error getting import store ${id}: ${statusCode} ${msg.msg}")
        })
      case None =>
        throw new Exception(s"Did not find the import store ${id}")
    }
  }
}
