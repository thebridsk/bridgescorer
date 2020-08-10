package com.github.thebridsk.bridge.data

import com.github.thebridsk.bridge.data.SystemTime.Timestamp

import io.swagger.v3.oas.annotations.media.Schema

case class MatchPlayerPosition(
    north: String,
    south: String,
    east: String,
    west: String,
    allspecified: Boolean,
    gotns: Boolean,
    gotew: Boolean
)

@Schema(description = "A duplicate match, version 2 (old version)")
case class MatchDuplicateV2(
    @Schema(description = "The ID of the MatchDuplicate", required = true)
    id: MatchDuplicate.Id,
    @Schema(
      description = "The teams playing the match, the key is the team ID",
      required = true
    )
    teams: Map[Team.Id, Team],
    @Schema(
      description =
        "The duplicate boards of the match, the key is the board ID",
      required = true
    )
    boards: Map[Board.Id, BoardV1],
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
    updated: Timestamp
) extends VersionedInstance[
      MatchDuplicate,
      MatchDuplicateV2,
      MatchDuplicate.Id
    ] {

  def equalsIgnoreModifyTime(
      other: MatchDuplicateV2,
      throwit: Boolean = false
  ): Boolean =
    id == other.id &&
      equalsInTeams(other, throwit) &&
      equalsInBoards(other, throwit)

  def equalsInTeams(
      other: MatchDuplicateV2,
      throwit: Boolean = false
  ): Boolean = {
    if (teams.keySet == other.teams.keySet) {
      // this function returns true if the values in the two maps are not equal
      teams.keys.find { key =>
        {
          teams.get(key) match {
            case Some(me) =>
              other.teams.get(key) match {
                case Some(ome) => !me.equalsIgnoreModifyTime(ome)
                case None      => true
              }
            case None =>
              other.teams.get(key) match {
                case Some(ome) =>
                  true
                case None =>
                  false
              }
          }
        }
      }.isEmpty
    } else {
      if (throwit)
        throw new Exception(
          "MatchDuplicateV2 teams don't have same key: " + teams.keySet + " " + other.teams.keySet
        )
      false
    }
  }

  def equalsInBoards(
      other: MatchDuplicateV2,
      throwit: Boolean = false
  ): Boolean = {
    if (boards.keySet == other.boards.keySet) {
      val notequalboard = boards.keys.find { key =>
        {
          // this function returns true if the values in the two maps are not equal
          boards.get(key) match {
            case Some(me) =>
              other.boards.get(key) match {
                case Some(ome) => !me.equalsIgnoreModifyTime(ome)
                case None      => true
              }
            case None =>
              other.boards.get(key) match {
                case Some(ome) =>
                  true
                case None =>
                  false
              }
          }
        }
      }
      if (throwit) notequalboard.foreach(key => {
        val b1 = boards.get(key)
        val b2 = other.boards.get(key)
        throw new Exception(
          "MatchDuplicateV2 boards don't same value for key: " + key + "\n  " + b1 + "\n  " + b2
        )
      })
      notequalboard.isEmpty
    } else {
      if (throwit)
        throw new Exception(
          "MatchDuplicateV2 boards don't have same key: " + teams.keySet + " " + other.teams.keySet
        )
      false
    }
  }

  def setId(
      newId: MatchDuplicate.Id,
      forCreate: Boolean,
      dontUpdateTime: Boolean = false
  ): MatchDuplicateV2 = {
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

  def copyForCreate(id: MatchDuplicate.Id): MatchDuplicateV2 = {
    val time = SystemTime.currentTimeMillis()
    val xteams = teams.map(e => (e._1 -> e._2.copyForCreate(e._1))).toMap
    val xboards = boards.map(e => (e._1 -> e._2.copyForCreate(e._1))).toMap
    copy(
      id = id,
      created = time,
      updated = time,
      teams = xteams,
      boards = xboards
    )

  }

  def updateBoard(board: BoardV1): MatchDuplicateV2 =
    copy(
      boards = boards + (board.id -> board),
      updated = SystemTime.currentTimeMillis()
    )

  def setBoards(boards: Map[Board.Id, BoardV1]): MatchDuplicateV2 = {
    copy(boards = boards, updated = SystemTime.currentTimeMillis())
  }

  def deleteBoard(boardid: Board.Id): MatchDuplicateV2 = {
    copy(boards = boards - boardid, updated = SystemTime.currentTimeMillis())
  }

  def updateHand(hand: DuplicateHandV1): MatchDuplicateV2 =
    boards.get(hand.board) match {
      case Some(board) => updateBoard(board.updateHand(hand))
      case None =>
        throw new IndexOutOfBoundsException(
          "Board " + hand.board + " not found"
        )
    }
  def updateHand(boardId: Board.Id, hand: DuplicateHandV1): MatchDuplicateV2 =
    boards.get(boardId) match {
      case Some(board) => updateBoard(board.updateHand(hand))
      case None =>
        throw new IndexOutOfBoundsException("Board " + boardId + " not found")
    }
  def updateHand(
      boardId: Board.Id,
      handId: Team.Id,
      hand: Hand
  ): MatchDuplicateV2 =
    boards.get(boardId) match {
      case Some(board) => updateBoard(board.updateHand(handId, hand))
      case None =>
        throw new IndexOutOfBoundsException("Board " + boardId + " not found")
    }

  def updateTeam(team: Team): MatchDuplicateV2 =
    copy(
      teams = teams + (team.id -> team),
      updated = SystemTime.currentTimeMillis()
    )

  def setTeams(teams: Map[Team.Id, Team]): MatchDuplicateV2 =
    copy(teams = teams, updated = SystemTime.currentTimeMillis())

  def deleteTeam(teamid: Team.Id): MatchDuplicateV2 =
    copy(teams = teams - teamid, updated = SystemTime.currentTimeMillis())

  def getHand(boardId: Board.Id, handId: Team.Id): Option[DuplicateHandV1] = {
    boards.get(boardId) match {
      case Some(board) => board.hands.get(handId)
      case None        => None
    }
  }

  def getHand(
      tableid: Table.Id,
      round: Int,
      boardId: Board.Id
  ): Option[DuplicateHandV1] = {
    boards.get(boardId) match {
      case Some(b) =>
        b.hands.values.find { h =>
          h.table == tableid && h.round == round
        }
      case _ => None
    }
  }

  def getHandsInRound(tableid: Table.Id, round: Int): List[DuplicateHandV1] = {
    boards.values
      .map { b =>
        b.hands.values.filter { h =>
          h.table == tableid && h.round == round
        }
      }
      .flatten
      .toList
  }

  /**
    * Get the player names for all the positions.
    * Also returns whether all the player names are known.
    * @return (north,south,east,west, allspecified)
    */
  def determinePlayerPosition(hand: DuplicateHandV1): MatchPlayerPosition =
    determinePlayerPositionFromCaller(hand, hand.nIsPlayer1, hand.eIsPlayer1)

  /**
    * Get the player names for all the positions.
    * Also returns whether all the player names are known.
    * @return (north,south,east,west, allspecified,gotns,gotew)
    */
  def determinePlayerPositionFromCaller(
      hand: DuplicateHandV1,
      pN1: Boolean,
      pE1: Boolean
  ): MatchPlayerPosition = {
    var n = "Unknown"
    var s = n
    var e = n
    var w = n
    var gotns = true
    var gotew = true
    teams.get(hand.nsTeam) match {
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
    teams.get(hand.ewTeam) match {
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
    * @return the new MatchDuplicateV2 object.
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
  ): MatchDuplicateV2 =
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
          boards = boards.map {
            case (bid, board) =>
              (bid -> board.copy(hands = board.hands.map {
                case (hid, hand) =>
                  (hid -> {
                    if (hand.table == tableid && hand.round == round)
                      hand.copy(
                        nIsPlayer1 = newNIsPlayer1,
                        eIsPlayer1 = newEIsPlayer1
                      )
                    else hand
                  })
              }))
          }
        )
      case None =>
        throw new IllegalArgumentException(
          "Did not find any hands for table " + tableid + " in round " + round
        )
    }

  /**
    * Correct the vulnerability in the boards and hands of this object.
    * @param correctVulnerability the correct vulnerabilities on the boards
    * @return The corrected MatchDuplicateV2 object and a list of messages describing what was changed
    */
  def fixVulnerability(
      correctVulnerability: MatchDuplicateV2
  ): (MatchDuplicateV2, List[String]) = {
    var msgs: List[String] = Nil
    val md: MatchDuplicateV2 = copy(boards = boards.map {
      case (id, board) =>
        val correctBoard = correctVulnerability.boards(id)
        if (
          correctBoard.ewVul == board.ewVul && correctBoard.nsVul == board.nsVul
        ) {
          (id, board)
        } else {
          msgs = "Fixed board " + id :: msgs
          val hands = board.hands.map {
            case (handid, hand) =>
              val newplayed = hand.played.map {
                case (key, h) =>
                  (
                    key,
                    h.copy(
                      nsVul = correctBoard.nsVul,
                      ewVul = correctBoard.ewVul
                    )
                  )
              }
              (handid, hand.copy(played = newplayed))
          }
          val newboard = board.copy(
            hands = hands,
            nsVul = correctBoard.nsVul,
            ewVul = correctBoard.ewVul
          )
          (id, newboard)
        }
    })

    (md, msgs)
  }

  def fillBoards(bs: BoardSet, mov: Movement): MatchDuplicateV2 = {
    val useteams: Map[Team.Id, Team] = {
      if (teams.size != mov.numberTeams) {
        MatchDuplicateV2.createTeams(mov.numberTeams)
      } else {
        teams
      }
    }

    val filledB = scala.collection.mutable.Map[Board.Id, BoardV1]()

    bs.boards.foreach { board =>
      val bb = BoardV1.create(
        Board.id(board.id),
        board.nsVul,
        board.ewVul,
        board.dealer,
        Map()
      )
      filledB += (bb.id -> bb)
    }
    mov.hands.foreach { htp =>
      val ew = Team.id(htp.ew)
      val ns = Team.id(htp.ns)
      htp.boards.foreach { b =>
        val hand = DuplicateHandV1.create(
          htp.tableid,
          htp.round,
          Board.id(b),
          ns,
          ew
        )

        val board = filledB.get(hand.board).get

        val hands = board.hands + (hand.id -> hand)
        filledB += board.id -> board.copy(hands = hands)
      }
    }

    copy(
      teams = useteams,
      boards = filledB.toMap.filter(e => !e._2.hands.isEmpty),
      boardset = bs.name,
      movement = mov.name,
      updated = SystemTime.currentTimeMillis()
    )
  }

  /**
    * Get all the table Ids in sort order.
    */
  @Schema(hidden = true)
  def getTableIds(): List[Table.Id] = {
    boards.values
      .flatMap(b => b.hands.values)
      .map(h => h.table)
      .map { id =>
        id.asInstanceOf[Table.Id]
      }
      .toSet
      .toList
      .sorted
  }

  @Schema(hidden = true)
  def getBoardSetObject(): BoardSetV1 = {
    val bins = boards.values
      .map { b =>
        b.getBoardInSet
      }
      .toList
      .sortWith((l, r) => l.id < r.id)
//     name: String, short: String, description: String, boards: List[BoardInSet]
    BoardSet(
      BoardSet.id(s"${boardset.id}In${id}"),
      "Used in match " + id,
      "Used in match " + id,
      bins
    )
  }

  def convertToCurrentVersion: (Boolean, MatchDuplicateV3) =
    (
      false,
      MatchDuplicateV3(
        id,
        teams.values.toList
          .sortWith((b1, b2) => b1.id < b2.id),
        boards.values
          .map { e =>
            e.convertToCurrentVersion
          }
          .toList
          .sortWith((b1, b2) => b1.id < b2.id),
        boardset,
        movement,
        created,
        updated
      )
    )

  def readyForWrite: MatchDuplicateV2 = this

}

object MatchDuplicateV2 {
  val time: Timestamp = SystemTime.currentTimeMillis()
  def create(
      id: MatchDuplicate.Id = MatchDuplicate.idNul,
      boardset: BoardSet.Id = BoardSet.default,
      movement: Movement.Id = Movement.default
  ) = new MatchDuplicateV2(id, Map(), Map(), boardset, movement, time, time)

  def createTeams(numberTeams: Int): Map[Team.Id, Team] = {
    (1 to numberTeams)
      .map(t => Team.id(t))
      .map(id => id -> Team.create(id, "", ""))
      .toMap
  }
}
