package com.github.thebridsk.bridge.server.service.graphql

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

import com.github.thebridsk.bridge.server.service.graphql.schema.SchemaQuery.{log => _, _}
import com.github.thebridsk.bridge.server.service.graphql.schema.SchemaMutation.{log => _, _}

object SchemaDefinition {

  val log = Logger(SchemaDefinition.getClass.getName)

  val BridgeScorerSchema = Schema(QueryType, Some(MutationType))
}
