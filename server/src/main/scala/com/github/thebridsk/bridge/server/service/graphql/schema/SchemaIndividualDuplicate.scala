package com.github.thebridsk.bridge.server.service.graphql.schema

import sangria.schema._
import scala.concurrent.Future
import com.github.thebridsk.bridge.server.backend.BridgeService

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.thebridsk.utilities.logging.Logger

import SchemaBase.{log => _, _}
import SchemaHand.{log => _, _}
import com.github.thebridsk.bridge.data.IndividualBoard
import com.github.thebridsk.bridge.data.IdIndividualBoard
import com.github.thebridsk.bridge.data.IndividualMovement
import com.github.thebridsk.bridge.data.IdIndividualMovement
import com.github.thebridsk.bridge.data.IdIndividualDuplicate
import com.github.thebridsk.bridge.data.IndividualDuplicate
import com.github.thebridsk.bridge.data.IdIndividualDuplicateHand
import com.github.thebridsk.bridge.data.IndividualDuplicateHand
import com.github.thebridsk.bridge.data.Id
import sangria.marshalling.playJson._
import play.api.libs.json._

object SchemaIndividualDuplicate {

  val log: Logger = Logger(SchemaDuplicate.getClass.getName)

  val IndividualMovementIdType: ScalarType[Id[IdIndividualMovement]] =
    idScalarType("IndividualMovementId", IndividualMovement)
  val IndividualBoardIdType: ScalarType[Id[IdIndividualBoard]] =
    idScalarType("IndividualBoardId", IndividualBoard)
  val IndividualDuplicateHandIdType: ScalarType[Id[IdIndividualDuplicateHand]] =
    idScalarType("IndividualDuplicateHandId", IndividualDuplicateHand)
  val IndividualDuplicateIdType: ScalarType[Id[IdIndividualDuplicate]] =
    idScalarType("IndividualDuplicateId", IndividualDuplicate)

  val IndividualDuplicateHandType: ObjectType[BridgeService, IndividualDuplicateHand] = ObjectType(
    "IndividualDuplicateHand",
    "A duplicate hand",
    fields[BridgeService, IndividualDuplicateHand](
      Field(
        "id",
        IndividualDuplicateHandIdType,
        Some("The id of the N player"),
        resolve = _.value.id
      ),
      Field(
        "played",
        OptionType(HandType),
        Some("The played hand"),
        resolve = _.value.played
      ),
      Field(
        "table",
        SchemaDuplicate.TableIdType,
        Some("The table the hand was played on"),
        resolve = _.value.table
      ),
      Field(
        "round",
        IntType,
        Some("The round the hand was played in"),
        resolve = _.value.round
      ),
      Field(
        "board",
        IndividualBoardIdType,
        Some("The board"),
        resolve = _.value.board
      ),
      Field(
        "north",
        IntType,
        Some("The north player"),
        resolve = _.value.north
      ),
      Field(
        "south",
        IntType,
        Some("The south player"),
        resolve = _.value.south
      ),
      Field(
        "east",
        IntType,
        Some("The east player"),
        resolve = _.value.east
      ),
      Field(
        "west",
        IntType,
        Some("The west player"),
        resolve = _.value.west
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

  val ArgIndividualHandId: Argument[Id[IdIndividualDuplicateHand]] = Argument(
    "id",
    IndividualDuplicateHandIdType,
    description = "The Id of the hand.  This the team id of the N player."
  )

  val IndividualDuplicateBoardType: ObjectType[BridgeService, IndividualBoard] = ObjectType(
    "IndividualDuplicateBoard",
    "A duplicate board",
    fields[BridgeService, IndividualBoard](
      Field(
        "id",
        IndividualBoardIdType,
        Some("The id of the board"),
        resolve = _.value.id
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
      Field("dealer", StringType, Some("The dealer"), resolve = _.value.dealer),
      Field(
        "hands",
        ListType(IndividualDuplicateHandType),
        Some("The played hand"),
        resolve = _.value.hands
      ),
      Field(
        "hand",
        OptionType(IndividualDuplicateHandType),
        Some("The played hand"),
        arguments = ArgIndividualHandId :: Nil,
        resolve = ctx => {
          val id = ctx arg ArgIndividualHandId
          ctx.value.getHand(id)
        }
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

  val ArgIndividualBoardId: Argument[Id[IdIndividualBoard]] =
    Argument("id", IndividualBoardIdType, description = "The Id of the board.")

  val IndividualDuplicateType
      : ObjectType[BridgeService, (BridgeService, IndividualDuplicate)] =
    ObjectType(
      "IndividualDuplicate",
      "An individual duplicate match",
      fields[BridgeService, (BridgeService, IndividualDuplicate)](
        Field(
          "id",
          IndividualDuplicateIdType,
          Some("The id of the duplicate match"),
          resolve = _.value._2.id
        ),
        Field(
          "players",
          ListType(StringType),
          Some("The players"),
          resolve = _.value._2.players
        ),
        Field(
          "boards",
          ListType(IndividualDuplicateBoardType),
          Some("The boards that were played"),
          resolve = _.value._2.boards
        ),
        Field(
          "boardset",
          SchemaDuplicate.BoardSetIdType,
          Some("The boardset that was used"),
          resolve = _.value._2.boardset
        ),
        Field(
          "movement",
          IndividualMovementIdType,
          Some("The movement that was used"),
          resolve = _.value._2.movement
        ),
        Field(
          "created",
          DateTimeType,
          Some("The time the team was last updated"),
          resolve = _.value._2.created
        ),
        Field(
          "updated",
          DateTimeType,
          Some("The time the team was last updated"),
          resolve = _.value._2.updated
        )
      )
    )

  val ArgIndividualDuplicateId: Argument[Id[IdIndividualDuplicate]] = Argument(
    "id",
    IndividualDuplicateIdType,
    description = "The Id of the duplicate match"
  )

  case class Player(i: Int, name: String)
  case class Players(players: List[Player])

  implicit val PlayerFormat = Json.format[Player]
  implicit val PlayersFormat = Json.format[Players]

  val PlayerType = ObjectType[BridgeService, Player](
    "Player",
    "An individual duplicate match",
    fields[BridgeService, Player](
      Field(
        "i",
        IntType,
        Some("The player's index"),
        resolve = _.value.i
      ),
      Field(
        "name",
        StringType,
        Some("The player's name"),
        resolve = _.value.name
      )
  ))

  val PlayerInputType = InputObjectType[Player](
    "Player",
    List(
      InputField(
        "i",
        IntType
      ),
      InputField(
        "name",
        StringType
      )
  ))

  val PlayersInputType = InputObjectType[Players]("Players", List(
    InputField("players", ListInputType(PlayerInputType))
  ))

  val ArgIndividualDuplicatePlayers: Argument[Players] = Argument(
    "players",
    PlayersInputType,
    description = "The updated players"
  )

}

object IndividualDuplicateAction {
  import SchemaIndividualDuplicate._

  def getDuplicate(
      ctx: Context[BridgeService, BridgeService]
  ): Future[(BridgeService, IndividualDuplicate)] = {
    val id = ctx arg ArgIndividualDuplicateId
    ctx.value.individualduplicates.read(id).map { rmd =>
      rmd match {
        case Right(md) =>
          (ctx.value, md)
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting match duplicate ${id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def getDuplicateFromRoot(
      ctx: Context[BridgeService, BridgeService]
  ): Future[IndividualDuplicate] = {
    val id = ctx arg ArgIndividualDuplicateId
    ctx.ctx.individualduplicates.read(id).map { rmd =>
      rmd match {
        case Right(md) => md
        case Left((statusCode, msg)) =>
          throw new Exception(
            s"Error getting match duplicate ${id}: ${statusCode} ${msg.msg}"
          )
      }
    }
  }

  def sort(
      list: List[IndividualDuplicate],
      sort: Option[Sort]
  ): List[IndividualDuplicate] = {
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
