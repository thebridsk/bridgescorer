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

object SchemaBase {

  val log = Logger( SchemaBase.getClass.getName )

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

  val DateTimeType = ScalarType[Timestamp]( "Timestamp",
    description = Some( "Timestamp, milliseconds since 1/1/1970."),
    coerceOutput = FloatType.coerceOutput(_,_),
    coerceUserInput = FloatType.coerceUserInput(_).map( v => v.asInstanceOf[Timestamp]),
    coerceInput = FloatType.coerceInput(_).map( v => v.asInstanceOf[Timestamp])
  )

  val PositionEnum = EnumType[PlayerPosition](
    "Position",
    Some("Player position at table"),
    List(
      EnumValue("N",
          value = North,
          description = Some("North player")
      ),
      EnumValue("S",
          value = South,
          description = Some("South player")
      ),
      EnumValue("E",
          value = East,
          description = Some("East player")
      ),
      EnumValue("W",
          value = West,
          description = Some("West player")
      ),
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

}
