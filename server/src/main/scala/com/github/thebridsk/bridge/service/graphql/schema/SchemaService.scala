package com.github.thebridsk.bridge.service.graphql.schema

import sangria.schema._
import com.github.thebridsk.bridge.backend.BridgeService
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.MatchChicago
import com.github.thebridsk.bridge.data.MatchRubber
import com.github.thebridsk.bridge.service.graphql.Data.ImportBridgeService
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Hand
import scala.concurrent.Future
import com.github.thebridsk.bridge.backend.BridgeService

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
import SchemaDuplicate.{log => _, _}
import SchemaRubber.{log => _, _}
import SchemaChicago.{log => _, _}

object SchemaService {

  val log = Logger(SchemaService.getClass.getName)

  val ImportIdType = idScalarTypeFromString[String]("ImportId")

  val ArgImportId =
    Argument("id", ImportIdType, description = "The Id of the import")

  val BridgeServiceFields = fields[BridgeService, BridgeService](
    Field(
      "id",
      ImportIdType,
      Some("The id of the bridge service"),
      resolve = _.value.id
    ),
    Field(
      "date",
      DateTimeType,
      Some("The date for the store."),
      resolve = _.value.getDate
    ),
    Field(
      "duplicate",
      OptionType(MatchDuplicateType),
      arguments = ArgDuplicateId :: Nil,
      resolve = DuplicateAction.getDuplicate
    ),
    Field(
      "duplicates",
      ListType(MatchDuplicateType),
      resolve = ctx =>
        ctx.value.duplicates.readAll().map { rall =>
          rall match {
            case Right(all) =>
              all.values.toList.map { md =>
                (
                  if (ctx.ctx.id == ctx.value.id) None else Some(ctx.value.id),
                  md
                )
              }
            case Left((statusCode, msg)) =>
              throw new Exception(
                s"Error getting MatchDuplicates: ${statusCode} ${msg.msg}"
              )
          }
        }
    ),
    Field(
      "duplicatesummaries",
      ListType(DuplicateSummaryType),
      arguments = ArgSort :: Nil,
      resolve = ctx =>
        ctx.value.getDuplicateSummaries().map { rmap =>
          rmap match {
            case Right(list) =>
              val argsort = ctx.arg(ArgSort)
              DuplicateAction.sortSummary(list, argsort).map { md =>
                (
                  if (ctx.ctx.id == ctx.value.id) None else Some(ctx.value.id),
                  md
                )
              }
            case Left((statusCode, msg)) =>
              throw new Exception(
                s"Error getting duplicate summaries: ${statusCode} ${msg.msg}"
              )
          }
        }
    ),
    Field(
      "duplicateIds",
      ListType(DuplicateIdType),
      resolve = _.value.duplicates.readAll().map { rall =>
        rall match {
          case Right(all) => all.keys.toList
          case Left((statusCode, msg)) =>
            throw new Exception(
              s"Error getting MatchDuplicates: ${statusCode} ${msg.msg}"
            )
        }
      }
    ),
    Field(
      "duplicateResult",
      OptionType(MatchDuplicateResultType),
      arguments = ArgDuplicateResultId :: Nil,
      resolve = DuplicateAction.getDuplicateResult
    ),
    Field(
      "duplicateResults",
      ListType(MatchDuplicateResultType),
      resolve = ctx =>
        ctx.value.duplicateresults.readAll().map { rall =>
          rall match {
            case Right(all) =>
              all.values.toList.map { md =>
                (
                  if (ctx.ctx.id == ctx.value.id) None else Some(ctx.value.id),
                  md
                )
              }
            case Left((statusCode, msg)) =>
              throw new Exception(
                s"Error getting MatchDuplicates: ${statusCode} ${msg.msg}"
              )
          }
        }
    ),
    Field(
      "duplicateResultIds",
      ListType(DuplicateResultIdType),
      resolve = _.ctx.duplicateresults.readAll().map { rmap =>
        rmap match {
          case Right(map) =>
            map.keys.toList
          case Left((statusCode, msg)) =>
            throw new Exception(
              s"Error getting duplicate ids: ${statusCode} ${msg.msg}"
            )
        }
      }
    ),
    Field(
      "duplicatestats",
      DuplicateStatsType,
      resolve = ctx => ctx.ctx
    ),
    Field(
      "rubber",
      OptionType(MatchRubberType),
      arguments = ArgRubberId :: Nil,
      resolve = RubberAction.getRubber
    ),
    Field(
      "rubbers",
      ListType(MatchRubberType),
      resolve = ctx =>
        ctx.value.rubbers.readAll().map { rall =>
          rall match {
            case Right(all) =>
              all.values.toList.map { md =>
                (
                  if (ctx.ctx.id == ctx.value.id) None else Some(ctx.value.id),
                  md
                )
              }
            case Left((statusCode, msg)) =>
              throw new Exception(
                s"Error getting MatchRubbers: ${statusCode} ${msg.msg}"
              )
          }
        }
    ),
    Field(
      "rubberIds",
      ListType(RubberIdType),
      resolve = _.value.rubbers.readAll().map { rall =>
        rall match {
          case Right(all) => all.keys.toList
          case Left((statusCode, msg)) =>
            throw new Exception(
              s"Error getting MatchRubbers: ${statusCode} ${msg.msg}"
            )
        }
      }
    ),
    Field(
      "chicago",
      OptionType(MatchChicagoType),
      arguments = ArgChicagoId :: Nil,
      resolve = ChicagoAction.getChicago
    ),
    Field(
      "chicagos",
      ListType(MatchChicagoType),
      resolve = ctx =>
        ctx.value.chicagos.readAll().map { rall =>
          rall match {
            case Right(all) =>
              all.values.toList.map { md =>
                (
                  if (ctx.ctx.id == ctx.value.id) None else Some(ctx.value.id),
                  md
                )
              }
            case Left((statusCode, msg)) =>
              throw new Exception(
                s"Error getting MatchChicagos: ${statusCode} ${msg.msg}"
              )
          }
        }
    ),
    Field(
      "chicagoIds",
      ListType(ChicagoIdType),
      resolve = _.value.chicagos.readAll().map { rall =>
        rall match {
          case Right(all) => all.keys.toList
          case Left((statusCode, msg)) =>
            throw new Exception(
              s"Error getting MatchChicagos: ${statusCode} ${msg.msg}"
            )
        }
      }
    ),
    Field(
      "duplicatesCount",
      IntType,
      Some("The id of the bridge service"),
      resolve = ctx => ctx.value.duplicates.size()
    ),
    Field(
      "duplicateresultsCount",
      IntType,
      Some("The id of the bridge service"),
      resolve = ctx => ctx.value.duplicateresults.size()
    ),
    Field(
      "chicagosCount",
      IntType,
      Some("The id of the bridge service"),
      resolve = ctx => ctx.value.chicagos.size()
    ),
    Field(
      "rubbersCount",
      IntType,
      Some("The id of the bridge service"),
      resolve = ctx => ctx.value.rubbers.size()
    )
  )

  val BridgeServiceType = ObjectType(
    "BridgeService",
    BridgeServiceFields
  )

}
object ServiceAction {
  import SchemaService._

  def getAllImportFromRoot(
      ctx: Context[BridgeService, BridgeService]
  ): Future[List[BridgeService]] = {
    ctx.ctx.importStore match {
      case Some(is) =>
        is.getAllIds().flatMap { rlids =>
          rlids match {
            case Right(lids) =>
              val fbss = lids.map { id =>
                is.get(id)
              }
              Future.foldLeft(fbss)(List[BridgeService]()) { (ac, v) =>
                v match {
                  case Right(bs) => bs :: ac
                  case Left(err) => ac
                }
              }
            case Left((statusCode, msg)) =>
              throw new Exception(
                s"Error getting all import IDs: ${statusCode} ${msg.msg}"
              )
          }
        }
      case None =>
        throw new Exception(s"Did not find the import store")
    }
  }

  def getImportFromRoot(
      ctx: Context[BridgeService, BridgeService]
  ): Future[BridgeService] = {
    val id = ctx arg ArgImportId
    ctx.ctx.importStore match {
      case Some(is) =>
        is.get(id)
          .map(
            rbs =>
              rbs match {
                case Right(bs) =>
                  bs
                case Left((statusCode, msg)) =>
                  log.fine(
                    s"Error getting import store ${id}: ${statusCode} ${msg.msg}"
                  )
//            throw new Exception(s"Error getting import store ${id}: ${statusCode} ${msg.msg}")
                  null
              }
          )
      case None =>
        throw new Exception(s"Did not find the import store ${id}")
    }
  }

}