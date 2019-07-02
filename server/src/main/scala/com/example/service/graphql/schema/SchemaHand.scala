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

object SchemaHand {

  val log = Logger(SchemaHand.getClass.getName)

  val HandType = ObjectType(
    "Hand",
    "Result of a bridge hand",
    fields[BridgeService, Hand](
      Field("id", StringType, Some("The id of the hand"), resolve = _.value.id),
      Field(
        "contractTricks",
        IntType,
        Some("The number of tricks in the contract."),
        resolve = _.value.contractTricks
      ),
      Field(
        "contractSuit",
        StringType,
        Some("The suit in the contract."),
        resolve = _.value.contractSuit
      ),
      Field(
        "contractDoubled",
        StringType,
        Some("The doubling of the contract."),
        resolve = _.value.contractDoubled
      ),
      Field(
        "declarer",
        PositionEnum,
        Some("The declarer of the contract."),
        resolve = ctx => PlayerPosition(ctx.value.declarer)
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
      Field(
        "madeContract",
        BooleanType,
        Some("Whether the contract was made or not."),
        resolve = _.value.madeContract
      ),
      Field(
        "tricks",
        IntType,
        Some("The number of tricks made or down in the contract."),
        resolve = _.value.tricks
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

}
