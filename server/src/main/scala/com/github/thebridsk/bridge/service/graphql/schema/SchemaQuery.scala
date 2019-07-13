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
