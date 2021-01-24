package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.Hidden
import scala.collection.View
import com.github.thebridsk.utilities.logging.Logger

@Schema(
  name = "IndividualDuplicate",
  title = "IndividualDuplicate - A duplicate match.",
  description = "A duplicate match, version 3 (current version)"
)
case class IndividualDuplicateV1 private (
    @Schema(description = "The ID of the IndividualDuplicate", required = true)
    id: IndividualDuplicate.Id,
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(
        description = "The players playing the match",
        required = true,
        implementation = classOf[String]
      ),
      arraySchema = new Schema(description = "All the teams.", required = true)
    )
    players: List[String],
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(
        description = "The teams playing the match",
        required = true,
        implementation = classOf[Team]
      ),
      arraySchema = new Schema(description = "All the teams.  If size is 0, then individual movements were used.", required = true)
    )
    teams: List[Team],
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(
        description = "The duplicate boards of the match",
        required = true,
        implementation = classOf[IndividualBoardV1]
      ),
      arraySchema = new Schema(
        description = "All the boards being played in this match.",
        required = true
      )
    )
    boards: List[IndividualBoardV1],
    @Schema(description = "The boardsets being used", required = true)
    boardset: BoardSet.Id,
    @Schema(description = "The movements being used", required = true)
    movement: IndividualMovement.Id,
    @Schema(
      description =
        "When the duplicate hand was created, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    created: Timestamp,
    @Schema(
      description =
        "When the duplicate hand was last updated, in milliseconds since 1/1/1970 UTC",
      required = true
    )
    updated: Timestamp,
    @Schema(
      description = "the scoring method used, default is MP",
      allowableValues = Array("MP", "IMP"),
      required = false,
      `type` = "string"
    )
    scoringmethod: Option[String] = None
) extends VersionedInstance[
      IndividualDuplicateV1,
      IndividualDuplicateV1,
      IndividualDuplicate.Id
    ] {

  def equalsIgnoreModifyTime(
      other: IndividualDuplicateV1,
      throwit: Boolean = false
  ): Boolean =
    equalsInId(other, throwit) &&
      equalsInPlayers(other, throwit) &&
      equalsInBoards(other, throwit)

  def equalsInId(other: IndividualDuplicateV1, throwit: Boolean = false): Boolean = {
    val rc = id == other.id
    if (!rc && throwit)
      throw new Exception(
        s"IndividualDuplicateV1 other did not have id equal to ${id}: ${other.id}"
      )
    rc
  }

  def equalsInBoards(
      other: IndividualDuplicateV1,
      throwit: Boolean = false
  ): Boolean = {
    if (boards.length == other.boards.length) {
      boards.find { t1 =>
        // this function must return true if t1 is NOT in other.team
        val rc = other.boards.find { t2 =>
          t1.id == t2.id && t1.equalsIgnoreModifyTime(t2)
        }.isEmpty
        if (rc && throwit)
          throw new Exception(
            "IndividualDuplicateV1 other did not have board equal to: " + t1
          )
        rc
      }.isEmpty
    } else {
      if (throwit)
        throw new Exception(
          "IndividualDuplicateV1 boards don't have same key: " + boards.map { t =>
            t.id
          } + " " + other.boards.map { t =>
            t.id
          }
        )
      false
    }
  }

  def equalsInPlayers(
      other: IndividualDuplicateV1,
      throwit: Boolean = false
  ): Boolean = {
    if (players == other.players) {
      return true
    } else if (throwit) {
      throw new Exception(
        s"IndividualDuplicateV1 players are not the same: ${players} ${other.players}"
      )
    } else {
      false
    }
  }

  def setId(
      newId: IndividualDuplicate.Id,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ): IndividualDuplicateV1 = {
    if (dontUpdateTime) {
      copy(id = newId)
    } else {
      val time = SystemTime.currentTimeMillis()
      copy(
        id = newId, /* created=if (forCreate) time; else created, */ updated =
          time
      )
    }
  }

  def copyForCreate(id: IndividualDuplicate.Id): IndividualDuplicateV1 = {
    val time = SystemTime.currentTimeMillis()
    val xplayers = players
    val xboards = boards.map(e => e.copyForCreate(e.id))
    copy(
      id = id,
      created = time,
      updated = time,
      players = xplayers,
      boards = xboards
    )

  }

  def updateBoard(board: IndividualBoardV1): IndividualDuplicateV1 = {
    val nb = boards.map { b =>
      if (b.id == board.id) (true, board) else (false, b)
    }
    val nb1 = nb.foldLeft((false, List[IndividualBoardV1]()))((ag, b) =>
      (ag._1 || b._1, b._2 :: ag._2)
    )
    val nb2 = if (nb1._1) nb1._2 else board :: nb1._2
    copy(
      boards = nb2.sortWith(IndividualBoardV1.sort),
      updated = SystemTime.currentTimeMillis()
    )
  }

  def setBoards(nboards: List[IndividualBoardV1]): IndividualDuplicateV1 = {
    copy(
      boards = nboards.sortWith(IndividualBoardV1.sort),
      updated = SystemTime.currentTimeMillis()
    )
  }

  def deleteBoard(boardid: IndividualBoard.Id): IndividualDuplicateV1 = {
    copy(
      boards = boards.filter { b =>
        b.id != boardid
      },
      updated = SystemTime.currentTimeMillis()
    )
  }

  def getBoard(boardid: IndividualBoard.Id): Option[IndividualBoardV1] = {
    boards.find { b =>
      b.id == boardid
    }
  }

  def updateHand(hand: IndividualDuplicateHandV1): IndividualDuplicateV1 =
    getBoard(hand.board) match {
      case Some(board) => updateBoard(board.updateHand(hand))
      case None =>
        throw new IndexOutOfBoundsException(
          "Board " + hand.board + " not found"
        )
    }
  def updateHand(boardId: IndividualBoard.Id, hand: IndividualDuplicateHandV1): IndividualDuplicateV1 =
    getBoard(boardId) match {
      case Some(board) => updateBoard(board.updateHand(hand))
      case None =>
        throw new IndexOutOfBoundsException("Board " + boardId + " not found")
    }
  def updateHand(
      boardId: IndividualBoard.Id,
      handId: IndividualDuplicateHandV1.Id,
      hand: Hand
  ): IndividualDuplicateV1 =
    getBoard(boardId) match {
      case Some(board) => updateBoard(board.updateHand(handId, hand))
      case None =>
        throw new IndexOutOfBoundsException("Board " + boardId + " not found")
    }

  /**
    * Get the specified player
    *
    * @param p the index of the player, one based.
    *          The value must be greater than 0 and less than or equal to the number of players.
    * @return
    */
  @Hidden
  def getPlayer(p: Int): String = {
    players(p-1)
  }

  def updatePlayer(p: Int, name: String): IndividualDuplicateV1 = {
    val np = players.zipWithIndex.map { e =>
      val (player,i0) = e
      if (i0+1 == p) name
      else player
    }
    copy(players = np)
  }

  def setPlayers(players: List[String]): IndividualDuplicateV1 = {
    copy(players=players)
  }

  def deletePlayer(p: Int): IndividualDuplicateV1 =
    updatePlayer(p,"")

  @Schema(hidden = true)
  def getPlayerName(i: Int): String = {
    val p = getPlayer(i)
    if (p == "") s"${i}"
    else p
  }

  @Schema(hidden = true)
  def getPlayerNames(): List[String] = {
    players.zipWithIndex.map { e =>
      val (p,i) = e
      if (p == "") s"${i+1}"
      else p
    }
  }

  /**
    * @param table
    * @param round
    * @return A hand played at the table and round, or None if not found.
    */
  def playersOnTableInRound(table: Table.Id, round: Int): Option[IndividualDuplicateHandV1] = {
    boards
      .flatMap(_.hands)
      .find(dh => dh.round == round && dh.table == table)
  }

  /**
    * @return the indexes in the order that would return the player's names in sorted order.
    */
  def sortedPlayers: List[Int] = {
    players
      .zipWithIndex
      .sortBy(_._1)
      .map(_._2 + 1)
  }

  def getHand(boardId: IndividualBoard.Id, handId: IndividualDuplicateHandV1.Id): Option[IndividualDuplicateHandV1] = {
    getBoard(boardId) match {
      case Some(board) => board.getHand(handId)
      case None        => None
    }
  }

  def getHand(
      tableid: Table.Id,
      round: Int,
      boardId: IndividualBoard.Id
  ): Option[IndividualDuplicateHandV1] = {
    getBoard(boardId) match {
      case Some(b) =>
        b.hands.find { h =>
          h.table == tableid && h.round == round
        }
      case _ => None
    }
  }

  def getHandsInRound(tableid: Table.Id, round: Int): List[IndividualDuplicateHandV1] = {
    boards
      .map { b =>
        b.hands.filter { h =>
          h.table == tableid && h.round == round
        }
      }
      .flatten
      .toList
  }

  def allDone: Boolean = {
    boards.find { b =>
      b.hands.find(h => !h.wasPlayed).isDefined
    }.isEmpty
  }

  def allPlayedHands: View[IndividualDuplicateHandV1] = {
    boards.view.flatMap { b =>
      b.hands.filter(dh => dh.wasPlayed)
    }
  }

  import IndividualDuplicateV1._
  @Hidden
  def isMP: Boolean =
    scoringmethod
      .map { sm =>
        sm == MatchPoints
      }
      .getOrElse(true)
  @Hidden
  def isIMP: Boolean =
    scoringmethod
      .map { sm =>
        sm == InternationalMatchPoints
      }
      .getOrElse(false)

  /**
    * Correct the vulnerability in the boards and hands of this object.
    * @param correctVulnerability the correct vulnerabilities on the boards
    * @return The corrected IndividualDuplicateV1 object and a list of messages describing what was changed
    */
  def fixVulnerability(
      correctVulnerability: IndividualDuplicateV1
  ): (IndividualDuplicateV1, List[String]) = {
    var msgs: List[String] = Nil
    val md: IndividualDuplicateV1 = copy(boards = boards.map { board =>
      val id = board.id
      val correctBoard = correctVulnerability.getBoard(id).get
      if (
        correctBoard.ewVul == board.ewVul && correctBoard.nsVul == board.nsVul
      ) {
        board
      } else {
        msgs = "Fixed board " + id :: msgs
        val hands = board.hands.map { hand =>
          val newplayed = hand.played.map { h =>
            h.copy(nsVul = correctBoard.nsVul, ewVul = correctBoard.ewVul)
          }
          hand.copy(played = newplayed)
        }
        val newboard = board.copy(
          hands = hands,
          nsVul = correctBoard.nsVul,
          ewVul = correctBoard.ewVul
        )
        newboard
      }
    })

    (md, msgs)
  }

  def fillBoards(bs: BoardSet, mov: IndividualMovement): IndividualDuplicateV1 = {
    val time = SystemTime.currentTimeMillis()
    val useplayers: List[String] = List.fill(mov.numberPlayers)("")

    val brds = bs.boards.map(b => IndividualBoard.id(b.id) -> b).toMap
    val bbb = mov.hands
      .flatMap { htp =>
        htp.boards.map { b =>
          val bid = IndividualBoard.id(b)
          (
            IndividualDuplicateHandV1.create(
              htp.tableid,
              htp.round,
              bid,
              htp.north,
              htp.south,
              htp.east,
              htp.west,
            ),
            bid
          )
        }
      }
      .groupBy { e =>
        e._2
      }
      .flatMap { e =>
        val (b, dhs) = e
        val hands = dhs.map { e =>
          e._1
        }
        if (!hands.isEmpty) {
          try {
            val board = brds.get(b).get
            Some(
              IndividualBoardV1(
                IndividualBoard.id(board.id),
                board.nsVul,
                board.ewVul,
                board.dealer,
                hands
              )
            )
          } catch {
            case x: NoSuchElementException =>
               log.fine(s"fillBoards trying to get board ${b} from brds ${brds.mkString}", x)
               throw new IllegalArgumentException(s"Boardset ${bs.name.id} does not have board ${b.toInt}")
          }
        } else {
          None
        }
      }
      .toList
      .sortWith(IndividualBoard.sort)

    copy(
      players = useplayers,
      boards = bbb,
      boardset = bs.name,
      movement = mov.name,
      created = time,
      updated = SystemTime.currentTimeMillis()
    )
  }

  @Schema(hidden = true)
  def getScoringMethod: String =
    scoringmethod.getOrElse(IndividualDuplicateV1.MatchPoints)

  /**
    * Get all the table Ids in sort order.
    */
  @Schema(hidden = true)
  def getTableIds(): List[Table.Id] = {
    boards
      .flatMap(b => b.hands)
      .map(h => h.table)
      .map { id =>
        id.asInstanceOf[Table.Id]
      }
      .toSet
      .toList
      .sortWith((l, r) => l < r)
  }

  @Schema(hidden = true)
  def getBoardSetObject(): BoardSetV1 = {
    val bins = boards
      .map { b =>
        b.getBoardInSet
      }
      .toList
      .sortWith((l,r) => l.id < r.id)
//     name: String, short: String, description: String, boards: List[BoardInSet]
    BoardSet(
      BoardSet.id(s"${boardset.id}In${id}"),
      "Used in match " + id,
      "Used in match " + id,
      bins
    )
  }

  def anyHandsPlayed: Boolean = {
    boards.find { b =>
      b.hands.find(h => h.wasPlayed).isDefined
    }.isDefined
  }

  def numberPlayedHands: Int = {
    boards
      .map { b =>
        b.hands.filter(h => h.wasPlayed).length
      }
      .foldLeft(0)((ac, v) => ac + v)
  }

  def convertToCurrentVersion: (Boolean, IndividualDuplicateV1) =
    (
      true,
      this
    )

  def readyForWrite: IndividualDuplicateV1 = this

}

trait IdIndividualDuplicate extends IdDuplicateSummary

object IndividualDuplicateV1 extends HasId[IdIndividualDuplicate]("I") {

  def create(): IndividualDuplicateV1 = {
    create(IndividualDuplicate.idNul)
  }

  def create(id: IndividualDuplicate.Id): IndividualDuplicateV1 = {
    val time = SystemTime.currentTimeMillis()
    new IndividualDuplicateV1(
      id,
      List(),
      List(),
      List(),
      BoardSet.idNul,
      IndividualMovement.idNul,
      time,
      time
    )
  }

  val MatchPoints = "MP"
  val InternationalMatchPoints = "IMP"

  def create(
      id: IndividualDuplicate.Id,
      players: List[String],
      boards: List[IndividualBoardV1],
      boardset: BoardSet.Id,
      movement: IndividualMovement.Id,
      created: Timestamp = SystemTime.UseCurrent,
      updated: Timestamp = SystemTime.UseCurrent
  ): IndividualDuplicateV1 = {
    val (cre,upd) = SystemTime.defaultTime(created,updated)
    new IndividualDuplicateV1(
      id,
      players,
      List(),
      boards.sortWith(IndividualBoard.sort),
      boardset,
      movement,
      cre,
      upd
    )
  }

  private[data] val log = Logger[IndividualDuplicateV1]()
}
