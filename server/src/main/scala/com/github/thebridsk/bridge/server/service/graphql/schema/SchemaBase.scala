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
import com.github.thebridsk.bridge.data.bridge.PositionUndefined
import com.github.thebridsk.bridge.data.RubberHand
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.ChicagoBestMatch
import com.github.thebridsk.bridge.data.RubberBestMatch

object SchemaBase {

  val log = Logger(SchemaBase.getClass.getName)

  def getPos(v: ast.Value): Option[AstLocation] = v match {
    case ast.IntValue(value, comments, position)        => position
    case ast.BigIntValue(value, comments, position)     => position
    case ast.FloatValue(value, comments, position)      => position
    case ast.BigDecimalValue(value, comments, position) => position
    case ast.StringValue(value, block, blockRawValue, comments, position) =>
      position
    case ast.BooleanValue(value, comments, position) => position
    case ast.EnumValue(value, comments, position)    => position
    case ast.ListValue(values, comments, position)   => position
    case ast.VariableValue(name, comments, position) => position
    case ast.NullValue(comments, position)           => position
    case ast.ObjectValue(fields, comments, position) => position
  }

  case class IdCoercionViolation(typename: String, extra: Option[String] = None)
      extends ValueCoercionViolation(s"""${typename} string expected ${extra
        .map(s => ": " + s)
        .getOrElse("")}""")

  def idScalarTypeFromString[T](typename: String) =
    ScalarType[T](
      typename,
      coerceOutput = (d, caps) => d.toString,
      coerceUserInput = {
        case s: String => Right(s.asInstanceOf[T])
        case _         => Left(IdCoercionViolation(typename))
      },
      coerceInput = {
        case ast.StringValue(s, _, _, _, _) => Right(s.asInstanceOf[T])
        case x =>
          Left(
            IdCoercionViolation(
              typename,
              getPos(x).map(
                p =>
                  s"at line ${p.line} column ${p.column}, sourceId ${p.sourceId} index ${p.index}"
              )
            )
          )
      }
    )

  val DateTimeType = ScalarType[Timestamp](
    "Timestamp",
    description = Some("Timestamp, milliseconds since 1/1/1970."),
    coerceOutput = FloatType.coerceOutput(_, _),
    coerceUserInput =
      FloatType.coerceUserInput(_).map(v => v.asInstanceOf[Timestamp]),
    coerceInput = FloatType.coerceInput(_).map(v => v.asInstanceOf[Timestamp])
  )

  val PositionEnum = EnumType[PlayerPosition](
    "Position",
    Some("Player position at table"),
    List(
      EnumValue("N", value = North, description = Some("North player")),
      EnumValue("S", value = South, description = Some("South player")),
      EnumValue("E", value = East, description = Some("East player")),
      EnumValue("W", value = West, description = Some("West player")),
      EnumValue("_", value = PositionUndefined, description = Some("Undefined position")),
    )
  )

  trait Sort
  case object SortCreated extends Sort
  case object SortCreatedDescending extends Sort
  case object SortId extends Sort

  val SortEnum = EnumType[Sort](
    "Sort",
    Some("how the result should be sorted"),
    List(
      EnumValue(
        "created",
        value = SortCreated,
        description = Some("Sort by created field, ascending.")
      ),
      EnumValue(
        "reversecreated",
        value = SortCreatedDescending,
        description = Some("Sort by created field, descending.")
      ),
      EnumValue("id", value = SortId, description = Some("Sort by Id"))
    )
  )

  val ArgSort = Argument(
    "sort",
    OptionInputType(SortEnum),
    description =
      "If specified, identifies the sort order of the values in list."
  )

}
