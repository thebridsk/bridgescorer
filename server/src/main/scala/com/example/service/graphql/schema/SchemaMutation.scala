package com.example.service.graphql.schema

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
import com.example.data.duplicate.stats.PlayerComparisonStats
import com.example.data.duplicate.stats.PlayerComparisonStat
import com.example.data.bridge.PlayerPosition
import com.example.data.bridge.North
import com.example.data.bridge.South
import com.example.data.bridge.East
import com.example.data.bridge.West
import com.example.data.RubberHand
import com.example.data.Round
import com.example.data.ChicagoBestMatch
import com.example.data.RubberBestMatch

import SchemaBase.{log => _, _}
import SchemaHand.{log => _, _}
import SchemaDuplicate.{log => _, _}
import SchemaRubber.{log => _, _}
import SchemaChicago.{log => _, _}
import SchemaService.{log => _, _}

object SchemaMutation {

  val log = Logger(SchemaMutation.getClass.getName)

  val MutationImportType = ObjectType(
    "MutationImport",
    fields[BridgeService, BridgeService](
      Field(
        "id",
        ImportIdType,
        Some("The id of the import store"),
        resolve = _.value.id
      ),
      Field(
        "importduplicate",
        MatchDuplicateType,
        description = Some("Import a duplicate match."),
        arguments = ArgDuplicateId :: Nil,
        resolve = ctx =>
          ImportAction.importDuplicate(ctx).map { md =>
            (None, md)
          }
      ),
      Field(
        "importduplicateresult",
        MatchDuplicateResultType,
        description = Some("Import a duplicate result."),
        arguments = ArgDuplicateResultId :: Nil,
        resolve = ctx =>
          ImportAction.importDuplicateResult(ctx).map { md =>
            (None, md)
          }
      ),
      Field(
        "importrubber",
        MatchRubberType,
        description = Some("Import a rubber match."),
        arguments = ArgRubberId :: Nil,
        resolve = ctx =>
          ImportAction.importRubber(ctx).map { md =>
            (None, md)
          }
      ),
      Field(
        "importchicago",
        MatchChicagoType,
        description = Some("Import a chicago match."),
        arguments = ArgChicagoId :: Nil,
        resolve = ctx =>
          ImportAction.importChicago(ctx).map { md =>
            (None, md)
          }
      ),
      Field(
        "delete",
        BooleanType,
        description = Some("Delete the import store"),
        resolve = ImportAction.deleteImportStore
      )
    )
  )

  val MutationType = ObjectType(
    "Mutation",
    fields[BridgeService, BridgeService](
      Field(
        "import",
        OptionType(MutationImportType),
        description = Some(
          "Selecting the import store from which imports are done.  returns null if not found."
        ),
        arguments = ArgImportId :: Nil,
        resolve = ServiceAction.getImportFromRoot
      )
    )
  )

}

object ImportAction {
  import SchemaMutation._

  def importDuplicate(
      ctx: Context[BridgeService, BridgeService]
  ): Future[MatchDuplicate] = {
    val dupId = ctx arg ArgDuplicateId
    val bs = ctx.value
    bs.duplicates.read(dupId).flatMap { rdup =>
      rdup match {
        case Right(dup) =>
          ctx.ctx.duplicates.importChild(dup).map { rc =>
            rc match {
              case Right(cdup) =>
                cdup
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error importing into store: ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
                )
            }
          }
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def importDuplicateResult(
      ctx: Context[BridgeService, BridgeService]
  ): Future[MatchDuplicateResult] = {
    val dupId = ctx arg ArgDuplicateId
    val bs = ctx.value
    bs.duplicateresults.read(dupId).flatMap { rdup =>
      rdup match {
        case Right(dup) =>
          ctx.ctx.duplicateresults.importChild(dup).map { rc =>
            rc match {
              case Right(cdup) =>
                cdup
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error importing into store: ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
                )
            }
          }
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting ${dupId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def importRubber(
      ctx: Context[BridgeService, BridgeService]
  ): Future[MatchRubber] = {
    val rubId = ctx arg ArgRubberId
    val bs = ctx.value
    bs.rubbers.read(rubId).flatMap { rdup =>
      rdup match {
        case Right(rub) =>
          ctx.ctx.rubbers.importChild(rub).map { rc =>
            rc match {
              case Right(cdup) =>
                cdup
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error importing into store: ${rubId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
                )
            }
          }
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting ${rubId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def importChicago(
      ctx: Context[BridgeService, BridgeService]
  ): Future[MatchChicago] = {
    val chiId = ctx arg ArgChicagoId
    val bs = ctx.value
    bs.chicagos.read(chiId).flatMap { rdup =>
      rdup match {
        case Right(chi) =>
          ctx.ctx.chicagos.importChild(chi).map { rc =>
            rc match {
              case Right(cdup) =>
                cdup
              case Left((statusCode, msg)) =>
                throw new Exception(
                  s"Error importing into store: ${chiId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
                )
            }
          }
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting ${chiId} from import store ${bs.id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def deleteImportStore(
      ctx: Context[BridgeService, BridgeService]
  ): Future[Boolean] = {
    val todelete = ctx.value
    ctx.ctx.importStore match {
      case Some(is) =>
        is.delete(todelete.id)
          .map { rd =>
            rd match {
              case Right(x) =>
                log.warning(s"Deleted import store ${todelete.id}")
                true
              case Left(error) =>
                log.warning(
                  s"Error deleting import store ${todelete.id}: ${error}"
                )
                false
            }
          }
          .recover {
            case x: Exception =>
              log.warning(s"Error deleting import store ${todelete.id}", x)
              false
          }
      case None =>
        log.fine("Did not find import store")
        Future.successful(false)
    }
  }

}
