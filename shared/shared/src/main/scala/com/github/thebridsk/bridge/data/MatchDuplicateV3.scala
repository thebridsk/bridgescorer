package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.Hidden
import scala.collection.View

@Schema(
  name = "MatchDuplicate",
  title = "MatchDuplicate - A duplicate match.",
  description = "A duplicate match, version 3 (current version)"
)
case class MatchDuplicateV3 private (
    @Schema(description = "The ID of the MatchDuplicate", required = true)
    id: MatchDuplicate.Id,
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(
        description = "The teams playing the match",
        required = true,
        implementation = classOf[Team]
      ),
      arraySchema = new Schema(description = "All the teams.", required = true)
    )
    teams: List[Team],
    @ArraySchema(
      minItems = 0,
      uniqueItems = true,
      schema = new Schema(
        description = "The duplicate boards of the match",
        required = true,
        implementation = classOf[BoardV2]
      ),
      arraySchema = new Schema(
        description = "All the boards being played in this match.",
        required = true
      )
    )
    boards: List[BoardV2],
    @Schema(description = "The boardsets being used", required = true)
    boardset: BoardSet.Id,
    @Schema(description = "The movements being used", required = true)
    movement: Movement.Id,
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
) extends VersionedInstance[MatchDuplicate, MatchDuplicateV3, MatchDuplicate.Id] {

  def equalsIgnoreModifyTime(
      other: MatchDuplicateV3,
      throwit: Boolean = false
  ) =
    equalsInId(other, throwit) &&
      equalsInTeams(other, throwit) &&
      equalsInBoards(other, throwit)

  def equalsInId(other: MatchDuplicateV3, throwit: Boolean = false) = {
    val rc = id == other.id
    if (!rc && throwit)
      throw new Exception(
        s"MatchDuplicateV3 other did not have id equal to ${id}: ${other.id}"
      )
    rc
  }

  def equalsInTeams(other: MatchDuplicateV3, throwit: Boolean = false) = {
    if (teams.length == other.teams.length) {
      teams.find { t1 =>
        // this function must return true if t1 is NOT in other.team
        val rc = other.teams.find { t2 =>
          t1.equalsIgnoreModifyTime(t2)
        }.isEmpty
        if (rc && throwit)
          throw new Exception(
            "MatchDuplicateV3 other did not have team equal to: " + t1
          )
        rc
      }.isEmpty
    } else {
      if (throwit)
        throw new Exception(
          "MatchDuplicateV3 teams don't have same key: " + teams.map { t =>
            t.id
          } + " " + other.teams.map { t =>
            t.id
          }
        )
      false
    }
  }

  def equalsInBoards(other: MatchDuplicateV3, throwit: Boolean = false) = {
    if (boards.length == other.boards.length) {
      boards.find { t1 =>
        // this function must return true if t1 is NOT in other.team
        val rc = other.boards.find { t2 =>
          t1.id == t2.id && t1.equalsIgnoreModifyTime(t2)
        }.isEmpty
        if (rc && throwit)
          throw new Exception(
            "MatchDuplicateV3 other did not have board equal to: " + t1
          )
        rc
      }.isEmpty
    } else {
      if (throwit)
        throw new Exception(
          "MatchDuplicateV3 boards don't have same key: " + boards.map { t =>
            t.id
          } + " " + other.boards.map { t =>
            t.id
          }
        )
      false
    }
  }

  def setId(
      newId: MatchDuplicate.Id,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ) = {
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

  def copyForCreate(id: MatchDuplicate.Id) = {
    val time = SystemTime.currentTimeMillis()
    val xteams = teams.map(e => e.copyForCreate(e.id))
    val xboards = boards.map(e => e.copyForCreate(e.id))
    copy(
      id = id,
      created = time,
      updated = time,
      teams = xteams,
      boards = xboards
    )

  }

  def updateBoard(board: BoardV2): MatchDuplicateV3 = {
    val nb = boards.map { b =>
      if (b.id == board.id) (true, board) else (false, b)
    }
    val nb1 = nb.foldLeft((false, List[BoardV2]()))(
      (ag, b) => (ag._1 || b._1, b._2 :: ag._2)
    )
    val nb2 = if (nb1._1) nb1._2 else board :: nb1._2
    copy(
      boards = nb2.sortWith(MatchDuplicateV3.sort),
      updated = SystemTime.currentTimeMillis()
    )
  }

  def setBoards(nboards: List[BoardV2]) = {
    copy(
      boards = nboards.sortWith(MatchDuplicateV3.sort),
      updated = SystemTime.currentTimeMillis()
    )
  }

  def deleteBoard(boardid: Board.Id) = {
    copy(boards = boards.filter { b =>
      b.id != boardid
    }, updated = SystemTime.currentTimeMillis())
  }

  def getBoard(boardid: Board.Id) = {
    boards.find { b =>
      b.id == boardid
    }
  }

  def updateHand(hand: DuplicateHandV2): MatchDuplicateV3 =
    getBoard(hand.board) match {
      case Some(board) => updateBoard(board.updateHand(hand))
      case None =>
        throw new IndexOutOfBoundsException(
          "Board " + hand.board + " not found"
        )
    }
  def updateHand(boardId: Board.Id, hand: DuplicateHandV2): MatchDuplicateV3 =
    getBoard(boardId) match {
      case Some(board) => updateBoard(board.updateHand(hand))
      case None =>
        throw new IndexOutOfBoundsException("Board " + boardId + " not found")
    }
  def updateHand(
      boardId: Board.Id,
      handId: Team.Id,
      hand: Hand
  ): MatchDuplicateV3 = getBoard(boardId) match {
    case Some(board) => updateBoard(board.updateHand(handId, hand))
    case None =>
      throw new IndexOutOfBoundsException("Board " + boardId + " not found")
  }

  def updateTeam(team: Team): MatchDuplicateV3 = {
    copy(teams = teams.map { t =>
      if (t.id == team.id) team else t
    }, updated = SystemTime.currentTimeMillis())
  }

  def setTeams(nteams: List[Team]) =
    copy(
      teams = nteams.sortWith(MatchDuplicateV3.sort),
      updated = SystemTime.currentTimeMillis()
    )

  def deleteTeam(teamid: Team.Id) =
    copy(
      teams = teams.filter(t => t.id != teamid),
      updated = SystemTime.currentTimeMillis()
    )

  def getHand(boardId: Board.Id, handId: Team.Id) = {
    getBoard(boardId) match {
      case Some(board) => board.getHand(handId)
      case None        => None
    }
  }

  def getHand(tableid: Table.Id, round: Int, boardId: Board.Id) = {
    getBoard(boardId) match {
      case Some(b) =>
        b.hands.find { h =>
          h.table == tableid && h.round == round
        }
      case _ => None
    }
  }

  def getHandsInRound(tableid: Table.Id, round: Int) = {
    boards
      .map { b =>
        b.hands.filter { h =>
          h.table == tableid && h.round == round
        }
      }
      .flatten
      .toList
  }

  def allPlayedHands: View[DuplicateHand] = {
    boards.view.flatMap { b =>
      b.hands.filter(dh => dh.wasPlayed)
    }
  }

  /**
    * Get the player names for all the positions.
    * Also returns whether all the player names are known.
    * @return (north,south,east,west, allspecified)
    */
  def determinePlayerPosition(hand: DuplicateHandV2): MatchPlayerPosition =
    determinePlayerPositionFromCaller(hand, hand.nIsPlayer1, hand.eIsPlayer1)

  def getTeam(id: Team.Id) = {
    teams.find(t => t.id == id)
  }

  /**
    * Get the player names for all the positions.
    * Also returns whether all the player names are known.
    * @return (north,south,east,west, allspecified,gotns,gotew)
    */
  def determinePlayerPositionFromCaller(
      hand: DuplicateHandV2,
      pN1: Boolean,
      pE1: Boolean
  ): MatchPlayerPosition = {
    var n = "Unknown"
    var s = n
    var e = n
    var w = n
    var gotns = true
    var gotew = true
    getTeam(hand.nsTeam) match {
      case Some(team) =>
        gotns = team.areBothPlayersSet()
        if (pN1) {
          n = team.player1
          s = team.player2
        } else {
          s = team.player1
          n = team.player2
        }
      case None =>
        gotns = false
    }
    getTeam(hand.ewTeam) match {
      case Some(team) =>
        gotew = team.areBothPlayersSet()
        if (pE1) {
          e = team.player1
          w = team.player2
        } else {
          w = team.player1
          e = team.player2
        }
      case None =>
        gotew = false
    }
    val all = gotns && gotew
    MatchPlayerPosition(n, s, e, w, all, gotns, gotew)
  }

  /**
    * Get the player names for all the positions.
    * Also returns whether all the player names are known.
    * It uses a random board to make the determination.
    * @return (north,south,east,west, allspecified)
    */
  def determinePlayerPositionFromRound(
      tableid: Table.Id,
      round: Int
  ): MatchPlayerPosition = {
    getHandsInRound(tableid, round).headOption match {
      case Some(hand) => determinePlayerPosition(hand)
      case None       => MatchPlayerPosition("", "", "", "", false, false, false)
    }
  }

  /**
    * Get the player names for all the positions.
    * Also returns whether all the player names are known.
    * It uses a random board to make the determination.
    * @return (north,south,east,west, allspecified)
    */
  def determinePlayerPositionFromBoard(
      tableid: Table.Id,
      round: Int,
      boardId: Board.Id
  ): MatchPlayerPosition = {
    getHand(tableid, round, boardId) match {
      case Some(hand) => determinePlayerPosition(hand)
      case None       => MatchPlayerPosition("", "", "", "", false, false, false)
    }
  }

  /**
    * Set the hands in a round with the new player positions.
    * This can not be used to change the names of the players.
    * It can only be used to switch the north and south players and
    * the east and west players.
    *
    * @return the new MatchDuplicateV3 object.
    * @throws IllegalArgumentException if the player names don't match,
    * or if there are no hands for the specified table and round
    */
  def setPlayerPositionForRound(
      tableid: Table.Id,
      round: Int,
      north: String,
      south: String,
      east: String,
      west: String
  ) =
    getHandsInRound(tableid, round).headOption match {
      case Some(hand) =>
        val MatchPlayerPosition(cn, cs, ce, cw, allplayed, gotns, gotew) =
          determinePlayerPosition(hand)
        val newNIsPlayer1 =
          if (cn == north && cs == south) hand.nIsPlayer1;
          else if (cs == north && cn == south) !hand.nIsPlayer1;
          else
            throw new IllegalArgumentException(
              "North and South players don't match"
            )
        val newEIsPlayer1 =
          if (ce == east && cw == west) hand.eIsPlayer1;
          else if (cw == east && ce == west) !hand.eIsPlayer1;
          else
            throw new IllegalArgumentException(
              "East and West players don't match"
            )
        copy(
          boards = boards.map { board =>
            board.copy(hands = board.hands.map { hand =>
              if (hand.table == tableid && hand.round == round)
                hand
                  .copy(nIsPlayer1 = newNIsPlayer1, eIsPlayer1 = newEIsPlayer1)
              else hand
            })
          }
        )
      case None =>
        throw new IllegalArgumentException(
          "Did not find any hands for table " + tableid + " in round " + round
        )
    }

  import MatchDuplicateV3._
  @Hidden
  def isMP =
    scoringmethod
      .map { sm =>
        sm == MatchPoints
      }
      .getOrElse(true)
  @Hidden
  def isIMP =
    scoringmethod
      .map { sm =>
        sm == InternationalMatchPoints
      }
      .getOrElse(false)

  /**
    * Correct the vulnerability in the boards and hands of this object.
    * @param correctVulnerability the correct vulnerabilities on the boards
    * @return The corrected MatchDuplicateV3 object and a list of messages describing what was changed
    */
  def fixVulnerability(
      correctVulnerability: MatchDuplicateV3
  ): (MatchDuplicateV3, List[String]) = {
    var msgs: List[String] = Nil
    val md: MatchDuplicateV3 = copy(boards = boards.map { board =>
      val id = board.id
      val correctBoard = correctVulnerability.getBoard(id).get
      if (correctBoard.ewVul == board.ewVul && correctBoard.nsVul == board.nsVul) {
        board
      } else {
        msgs = "Fixed board " + id :: msgs
        val hands = board.hands.map { hand =>
          val newplayed = hand.hand.map { h =>
            h.copy(nsVul = correctBoard.nsVul, ewVul = correctBoard.ewVul)
          }
          hand.copy(played = newplayed.toList)
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

  def fillBoards(bs: BoardSet, mov: Movement): MatchDuplicateV3 = {
    val useteams: List[Team] = {
      if (teams.size != mov.numberTeams) {
        MatchDuplicateV3.createTeams(mov.numberTeams)
      } else {
        teams
      }
    }

    val brds = bs.boards.map(b => Board.id(b.id) -> b).toMap
    val bbb = mov.hands
      .flatMap { htp =>
        val ew = Team.id(htp.ew)
        val ns = Team.id(htp.ns)
        htp.boards.map { b =>
          val bid = Board.id(b)
          (
            DuplicateHandV2.create(
              htp.tableid,
              htp.round,
              bid,
              ns,
              ew
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
          val board = brds.get(b).get
          Some(
            BoardV2.create(
              Board.id(board.id),
              board.nsVul,
              board.ewVul,
              board.dealer,
              hands
            )
          )
        } else {
          None
        }
      }
      .toList
      .sortWith(MatchDuplicateV3.sort)

    copy(
      teams = useteams.sortWith(MatchDuplicateV3.sort),
      boards = bbb,
      boardset = bs.name,
      movement = mov.name,
      updated = SystemTime.currentTimeMillis()
    )
  }

  @Schema(hidden = true)
  def getScoringMethod = scoringmethod.getOrElse(MatchDuplicateV3.MatchPoints)

  /**
    * Get all the table Ids in sort order.
    */
  @Schema(hidden = true)
  def getTableIds() = {
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
  def getBoardSetObject() = {
    val bins = boards
      .map { b =>
        b.getBoardInSet
      }
      .toList
      .sortWith(MatchDuplicateV3.sort)
//     name: String, short: String, description: String, boards: List[BoardInSet]
    BoardSet(
      BoardSet.id(s"${boardset.id}In${id}"),
      "Used in match " + id,
      "Used in match " + id,
      bins
    )
  }

  /**
    * Modify the player names according to the specified name map.
    * The timestamp is not changed.
    * @return None if the names were not changed.  Some() with the modified object
    */
  def modifyPlayers(nameMap: Map[String, String]) = {
    val (nteams, modified) = teams
      .map { t =>
        t.modifyPlayers(nameMap) match {
          case Some(nt) => (nt, true)
          case None     => (t, false)
        }
      }
      .foldLeft((List[Team](), false)) { (ac, v) =>
        (ac._1 ::: List(v._1), ac._2 || v._2)
      }
    if (modified) {
      Some(copy(teams = nteams))
    } else {
      None
    }
  }

  def numberPlayedHands = {
    boards
      .map { b =>
        b.hands.filter(h => h.wasPlayed).length
      }
      .foldLeft(0)((ac, v) => ac + v)
  }

  def convertToCurrentVersion =
    (
      true,
      MatchDuplicateV3(id, teams, boards, boardset, movement, created, updated)
    )

  def readyForWrite = this

}

trait IdMatchDuplicate extends IdDuplicateSummary

object MatchDuplicateV3 extends HasId[IdMatchDuplicate]("M") {
  def create(id: MatchDuplicate.Id = MatchDuplicate.idNul) = {
    val time = SystemTime.currentTimeMillis()
    new MatchDuplicateV3(id, List(), List(), BoardSet.idNul, Movement.idNul, time, time)
  }

  val MatchPoints = "MP"
  val InternationalMatchPoints = "IMP"

  def apply(
      id: MatchDuplicate.Id,
      teams: List[Team],
      boards: List[BoardV2],
      boardset: BoardSet.Id,
      movement: Movement.Id,
      created: Timestamp,
      updated: Timestamp
  ) = {
    new MatchDuplicateV3(
      id,
      teams.sortWith(sort),
      boards.sortWith(sort),
      boardset,
      movement,
      created,
      updated
    )
  }

  def createTeams(numberTeams: Int) = {
    (1 to numberTeams)
      .map(t => Team.id(t))
      .map(id => Team.create(id, "", ""))
      .toList
  }

  def sort(l: BoardInSet, r: BoardInSet) = l.id < r.id
  def sort(l: BoardV2, r: BoardV2) = l.id < r.id
  def sort(l: Team, r: Team) = l.id < r.id

}
