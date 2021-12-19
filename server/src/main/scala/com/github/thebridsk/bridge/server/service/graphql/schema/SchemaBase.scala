package com.github.thebridsk.bridge.server.service.graphql.schema

import sangria.schema._

import com.github.thebridsk.bridge.data.Id
import sangria.validation.ValueCoercionViolation
import sangria.ast
import com.github.thebridsk.utilities.time.SystemTime.Timestamp
import com.github.thebridsk.utilities.logging.Logger
import sangria.ast.AstLocation
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.bridge.North
import com.github.thebridsk.bridge.data.bridge.South
import com.github.thebridsk.bridge.data.bridge.East
import com.github.thebridsk.bridge.data.bridge.West
import com.github.thebridsk.bridge.data.HasId

object SchemaBase {

  val log: Logger = Logger(SchemaBase.getClass.getName)

  def getPos(v: ast.Value): Option[AstLocation] =
    v match {
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

  def idScalarTypeFromString[T](typename: String): ScalarType[T] =
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
              getPos(x).map(p =>
                s"at line ${p.line} column ${p.column}, sourceId ${p.sourceId} index ${p.index}"
              )
            )
          )
      }
    )

  def idScalarType[T](typename: String, hasId: HasId[T]): ScalarType[Id[T]] =
    ScalarType[Id[T]](
      typename,
      coerceOutput = (d, caps) => d.id,
      coerceUserInput = {
        case s: String => Right(hasId.id(s))
        case _         => Left(IdCoercionViolation(typename))
      },
      coerceInput = {
        case ast.StringValue(s, _, _, _, _) => Right(hasId.id(s))
        case x =>
          Left(
            IdCoercionViolation(
              typename,
              getPos(x).map(p =>
                s"at line ${p.line} column ${p.column}, sourceId ${p.sourceId} index ${p.index}"
              )
            )
          )
      }
    )

  val DateTimeType: ScalarType[Timestamp] = ScalarType[Timestamp](
    "Timestamp",
    description = Some("Timestamp, milliseconds since 1/1/1970."),
    coerceOutput = FloatType.coerceOutput(_, _),
    coerceUserInput =
      FloatType.coerceUserInput(_).map(v => v.asInstanceOf[Timestamp]),
    coerceInput = FloatType.coerceInput(_).map(v => v.asInstanceOf[Timestamp])
  )

  val PositionEnum: EnumType[PlayerPosition] = EnumType[PlayerPosition](
    "Position",
    Some("Player position at table"),
    List(
      EnumValue("N", value = North, description = Some("North player")),
      EnumValue("S", value = South, description = Some("South player")),
      EnumValue("E", value = East, description = Some("East player")),
      EnumValue("W", value = West, description = Some("West player"))
    )
  )

  trait Sort
  case object SortCreated extends Sort
  case object SortCreatedDescending extends Sort
  case object SortId extends Sort

  val SortEnum: EnumType[Sort] = EnumType[Sort](
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

  val ArgSort: Argument[Option[Sort]] = Argument(
    "sort",
    OptionInputType(SortEnum),
    description =
      "If specified, identifies the sort order of the values in list."
  )

}
