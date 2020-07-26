package com.github.thebridsk.bridge.data.sample

import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.Table
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import com.github.thebridsk.bridge.data.DuplicateHandV2

object TestMatchDuplicate {

  def getHand(
      dm: MatchDuplicate,
      boardid: Board.Id,
      handid: Team.Id,
      contractTricks: Int,
      contractSuit: String,
      contractDoubled: String,
      declarer: String,
      madeContract: Boolean,
      tricks: Int
  ): DuplicateHandV2 = {
    val b = dm.getBoard(boardid).get
    val dh = b.getHand(handid).get
    dh.updateHand(
      Hand.create(
        dh.id.id,
        contractTricks,
        contractSuit,
        contractDoubled,
        declarer,
        b.nsVul,
        b.ewVul,
        madeContract,
        tricks
      )
    )
  }

  val team1: Team.Id = Team.id(1)
  val team2: Team.Id = Team.id(2)
  val team3: Team.Id = Team.id(3)
  val team4: Team.Id = Team.id(4)

  def teams(): Map[Team.Id,Team] =
    List(
      Team.create(team1, "Nancy", "Norman"),
      Team.create(team2, "Ellen", "Edward"),
      Team.create(team3, "Susan", "Sam"),
      Team.create(team4, "Wilma", "Wayne")
    ).map( t => t.id -> t ).toMap

  def create(id: MatchDuplicate.Id): MatchDuplicate = {
    val ts: Map[Team.Id, Team] = teams()

    val boards = scala.collection.mutable.Map[Board.Id, Board]()

    def addBoard(board: Board) = boards += (board.id -> board)

    addBoard(Board.create(Board.id(1), false, false, "N", List()))
    addBoard(Board.create(Board.id(2), true, false, "E", List()))
    addBoard(Board.create(Board.id(7), true, true, "S", List()))
    addBoard(Board.create(Board.id(8), false, false, "W", List()))

    def addHand(hand: DuplicateHand) = {
      val board = boards.get(hand.board).get
      val hands = hand :: board.hands
      boards += (hand.board -> board.updateHand(hand))
    }

    addHand(DuplicateHand.create(Table.id(1), 1, Board.id(1), team1, team2))
    addHand(DuplicateHand.create(Table.id(1), 1, Board.id(2), team1, team2))
    addHand(DuplicateHand.create(Table.id(2), 2, Board.id(1), team3, team4))
    addHand(DuplicateHand.create(Table.id(2), 2, Board.id(2), team3, team4))
    addHand(DuplicateHand.create(Table.id(1), 3, Board.id(7), team3, team1))
    addHand(DuplicateHand.create(Table.id(1), 3, Board.id(8), team3, team1))
    addHand(DuplicateHand.create(Table.id(2), 4, Board.id(7), team2, team4))
    addHand(DuplicateHand.create(Table.id(2), 4, Board.id(8), team2, team4))

    MatchDuplicate(id, ts.values.toList, boards.values.toList, BoardSet.idNul, Movement.idNul, 0, 0)
  }

  def getHands(md: MatchDuplicate): List[DuplicateHand] = {
    var hands: List[DuplicateHand] = Nil
    hands = getHand(md, Board.id(1), team1, 3, "N", "N", "N", true, 5) :: hands
    hands = getHand(md, Board.id(2), team1, 4, "S", "N", "N", true, 5) :: hands
    hands = getHand(md, Board.id(1), team3, 4, "S", "N", "N", true, 5) :: hands
    hands = getHand(md, Board.id(2), team3, 4, "S", "N", "N", true, 5) :: hands

    hands = getHand(md, Board.id(7), team3, 4, "S", "N", "N", true, 5) :: hands
    hands = getHand(md, Board.id(8), team2, 4, "S", "N", "N", true, 5) :: hands
    hands.reverse
  }

  def getTeamScore(): Map[Team.Id,Int] = Map(team1 -> 3, team2 -> 1, team3 -> 1, team4 -> 3)

  def getPlayedMatch(dupid: MatchDuplicate.Id): MatchDuplicate = {
    var md = TestMatchDuplicate.create(dupid)
    val hands = TestMatchDuplicate.getHands(md)
    for (hand <- hands) {
      md = md.updateHand(hand)
    }
    md
  }

}
