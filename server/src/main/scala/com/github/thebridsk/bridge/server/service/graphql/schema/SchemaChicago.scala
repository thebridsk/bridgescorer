package com.github.thebridsk.bridge.server.service.graphql.schema

import sangria.schema._
import com.github.thebridsk.bridge.data.MatchChicago
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.BridgeService

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.data.DifferenceWrappers
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.Round
import com.github.thebridsk.bridge.data.ChicagoBestMatch
import com.github.thebridsk.bridge.data.IdMatchChicago

import SchemaBase.{log => _, _}
import SchemaHand.{log => _, _}
import com.github.thebridsk.bridge.data.Id

object SchemaChicago {

  val log: Logger = Logger(SchemaChicago.getClass.getName)

  val ChicagoIdType: ScalarType[Id[IdMatchChicago]] =
    idScalarType[IdMatchChicago]("ChicagoId", MatchChicago)

  val ChicagoRoundType: ObjectType[BridgeService, Round] = ObjectType(
    "ChicagoRound",
    "A chicago round",
    fields[BridgeService, Round](
      Field(
        "id",
        StringType,
        Some("The id of the chicago round"),
        resolve = _.value.id
      ),
      Field(
        "north",
        StringType,
        Some("The north player"),
        resolve = _.value.north
      ),
      Field(
        "south",
        StringType,
        Some("The south player"),
        resolve = _.value.south
      ),
      Field(
        "east",
        StringType,
        Some("The east player"),
        resolve = _.value.east
      ),
      Field(
        "west",
        StringType,
        Some("The west player"),
        resolve = _.value.west
      ),
      Field(
        "dealerFirstRound",
        PositionEnum,
        Some("The player that dealt the first hand"),
        resolve = ctx => PlayerPosition(ctx.value.dealerFirstRound)
      ),
      Field(
        "hands",
        ListType(HandType),
        Some("The played hands"),
        resolve = _.value.hands
      ),
      Field(
        "created",
        DateTimeType,
        Some("The time the match was last created"),
        resolve = _.value.created
      ),
      Field(
        "updated",
        DateTimeType,
        Some("The time the match was last updated"),
        resolve = _.value.updated
      )
    )
  )

  val ChicagoBestMatchType
      : ObjectType[BridgeService, (Option[String], ChicagoBestMatch)] =
    ObjectType(
      "ChicagoBestMatch",
      "Identifies the best match",
      fields[BridgeService, (Option[String], ChicagoBestMatch)](
        Field(
          "id",
          OptionType(ChicagoIdType),
          Some("The id of the best duplicate match from the main store"),
          resolve = _.value._2.id
        ),
        Field(
          "sameness",
          FloatType,
          Some("A percentage of similarity."),
          resolve = _.value._2.sameness
        ),
        Field(
          "differences",
          OptionType(ListType(StringType)),
          Some("The fields that are different"),
          resolve = _.value._2.differences
        )
      )
    )

  val MatchChicagoType
      : ObjectType[BridgeService, (Option[String], MatchChicago)] = ObjectType(
    "MatchChicago",
    "A rubber match",
    // Option string is the import ID, None for main store
    fields[BridgeService, (Option[String], MatchChicago)](
      Field(
        "id",
        ChicagoIdType,
        Some("The id of the rubber match"),
        resolve = _.value._2.id
      ),
      Field(
        "players",
        ListType(StringType),
        Some("The player names"),
        resolve = _.value._2.players
      ),
      Field(
        "rounds",
        ListType(ChicagoRoundType),
        Some("The rounds"),
        resolve = _.value._2.rounds
      ),
      Field(
        "gamesPerRound",
        IntType,
        Some("The number of hands per round, 1 if fast rotation"),
        resolve = _.value._2.gamesPerRound
      ),
      Field(
        "simpleRotation",
        BooleanType,
        Some(
          "Whether simple rotation is used, only valid if fast rotation is used"
        ),
        resolve = _.value._2.simpleRotation
      ),
      Field(
        "bestMatch",
        OptionType(ChicagoBestMatchType),
        Some("best match with main store"),
        resolve = ChicagoAction.getChicagoBestMatch
      ),
      Field(
        "created",
        DateTimeType,
        Some("The time the match was last created"),
        resolve = _.value._2.created
      ),
      Field(
        "updated",
        DateTimeType,
        Some("The time the match was last updated"),
        resolve = _.value._2.updated
      )
    )
  )

  val ArgChicagoId: Argument[Id[IdMatchChicago]] =
    Argument("id", ChicagoIdType, description = "The Id of the chicago match")

}

object ChicagoAction {
  import SchemaChicago._

  def getChicago(
      ctx: Context[BridgeService, BridgeService]
  ): Future[(Option[String], MatchChicago)] = {
    val id = ctx arg ArgChicagoId
    ctx.value.chicagos.read(id).map { rmd =>
      rmd match {
        case Right(md) =>
          (if (ctx.ctx.id == ctx.value.id) None else Some(ctx.value.id), md)
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting match chicago ${id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def getChicagoBestMatch(
      ctx: Context[BridgeService, (Option[String], MatchChicago)]
  ): Future[Option[(Option[String], ChicagoBestMatch)]] = {
    val mainStore = ctx.ctx
    val (importId, md) = ctx.value
    val sourcestore =
      if (importId.isEmpty) Future.successful(Some(mainStore))
      else
        mainStore.importStore.get.get(importId.get).map { rbs =>
          rbs match {
            case Right(bs)   => Some(bs)
            case Left(error) => None
          }
        }
    mainStore.chicagos.readAll().map { rlmd =>
      rlmd match {
        case Right(lmd) =>
          val x =
            lmd.values
              .map { mmd =>
                import DifferenceWrappers._
                val diff = md.difference("", mmd)
                log.fine(
                  s"Diff main(${mmd.id}) import(${importId},${md.id}): ${diff}"
                )
                ChicagoBestMatch(mmd.id, diff)
              }
              .foldLeft(ChicagoBestMatch.noMatch) { (ac, v) =>
                if (ac.sameness < v.sameness) v
                else ac
              }
          Some((importId, x))
        case Left(err) =>
          None
      }
    }
  }

  def getChicagoFromRoot(
      ctx: Context[BridgeService, BridgeService]
  ): Future[MatchChicago] = {
    val id = ctx arg ArgChicagoId
    ctx.ctx.chicagos.read(id).map { rmd =>
      rmd match {
        case Right(md) => md
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting match chicago ${id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def sortC(
      list: List[MatchChicago],
      sort: Option[Sort]
  ): List[MatchChicago] = {
    val l = sort
      .map { s =>
        s match {
          case SortCreated =>
            list.sortWith((l, r) => l.created < r.created)
          case SortCreatedDescending =>
            list.sortWith((l, r) => l.created > r.created)
          case SortId =>
            list.sortWith((l, r) => l.id < r.id)
        }
      }
      .getOrElse(list)
    log.info(s"""Returning list sorted with ${sort}: ${l
      .map(md => s"(${md.id},${md.created})")
      .mkString(",")}""")
    l
  }

}
