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

object SchemaQuery {

  val log = Logger(SchemaQuery.getClass.getName)

  val QueryType = ObjectType(
    "Query",
    fields[BridgeService, BridgeService](
      Field(
        "importIds",
        ListType(ImportIdType),
        resolve = _.ctx.importStore match {
          case Some(is) =>
            is.getAllIds()
              .map(
                rlist =>
                  rlist match {
                    case Right(list) =>
                      list
                    case Left((statusCode, msg)) =>
                      throw new Exception(
                        s"Error getting import store ids: ${statusCode} ${msg.msg}"
                      )
                  }
              )
          case None =>
            throw new Exception("Did not find the import store")
        }
      ),
      Field(
        "importsCount",
        IntType,
        resolve = _.ctx.importStore match {
          case Some(is) =>
            is.getAllIds()
              .map(
                rlist =>
                  rlist match {
                    case Right(list) =>
                      list.size
                    case Left((statusCode, msg)) =>
                      throw new Exception(
                        s"Error getting number of imports: ${statusCode} ${msg.msg}"
                      )
                  }
              )
          case None =>
            throw new Exception("Did not find the import store")
        }
      ),
      Field(
        "imports",
        ListType(BridgeServiceType),
        resolve = ServiceAction.getAllImportFromRoot
      ),
      Field(
        "import",
        OptionType(BridgeServiceType),
        arguments = ArgImportId :: Nil,
        resolve = ServiceAction.getImportFromRoot
      ),
      Field(
        "mainStore",
        OptionType(BridgeServiceType),
        resolve = ctx => ctx.ctx
      )
    ) ++
      BridgeServiceFields
  )

}
