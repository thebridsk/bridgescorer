package com.github.thebridsk.bridge.data.sample

import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.bridge.data.MatchDuplicate
import com.github.thebridsk.bridge.data.Team
import com.github.thebridsk.bridge.data.Board
import com.github.thebridsk.bridge.data.DuplicateHand
import com.github.thebridsk.bridge.data.Hand
import com.github.thebridsk.bridge.data.Table

object TestMatchDuplicate {

  def getHand(
      dm: MatchDuplicate,
      boardid: Id.DuplicateBoard,
      handid: Team.Id,
      contractTricks: Int,
      contractSuit: String,
      contractDoubled: String,
      declarer: String,
      madeContract: Boolean,
      tricks: Int
  ) = {
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

  val team1 = Team.id(1)
  val team2 = Team.id(2)
  val team3 = Team.id(3)
  val team4 = Team.id(4)

  def teams() =
    List(
      Team.create(team1, "Nancy", "Norman"),
      Team.create(team2, "Ellen", "Edward"),
      Team.create(team3, "Susan", "Sam"),
      Team.create(team4, "Wilma", "Wayne")
    ).map( t => t.id -> t ).toMap

  def create(id: Id.MatchDuplicate): MatchDuplicate = {
    val ts: Map[Team.Id, Team] = teams()

    val boards = scala.collection.mutable.Map[Id.DuplicateBoard, Board]()

    def addBoard(board: Board) = boards += (board.id -> board)

    addBoard(Board.create("B1", false, false, "N", List()))
    addBoard(Board.create("B2", true, false, "E", List()))
    addBoard(Board.create("B7", true, true, "S", List()))
    addBoard(Board.create("B8", false, false, "W", List()))

    def addHand(hand: DuplicateHand) = {
      val board = boards.get(hand.board).get
      val hands = hand :: board.hands
      boards += (hand.board -> board.updateHand(hand))
    }

    addHand(DuplicateHand.create(Table.id(1), 1, "B1", team1, team2))
    addHand(DuplicateHand.create(Table.id(1), 1, "B2", team1, team2))
    addHand(DuplicateHand.create(Table.id(2), 2, "B1", team3, team4))
    addHand(DuplicateHand.create(Table.id(2), 2, "B2", team3, team4))
    addHand(DuplicateHand.create(Table.id(1), 3, "B7", team3, team1))
    addHand(DuplicateHand.create(Table.id(1), 3, "B8", team3, team1))
    addHand(DuplicateHand.create(Table.id(2), 4, "B7", team2, team4))
    addHand(DuplicateHand.create(Table.id(2), 4, "B8", team2, team4))

    MatchDuplicate(id, ts.values.toList, boards.values.toList, "", "", 0, 0)
  }

  def getHands(md: MatchDuplicate) = {
    var hands: List[DuplicateHand] = Nil
    hands = getHand(md, "B1", team1, 3, "N", "N", "N", true, 5) :: hands
    hands = getHand(md, "B2", team1, 4, "S", "N", "N", true, 5) :: hands
    hands = getHand(md, "B1", team3, 4, "S", "N", "N", true, 5) :: hands
    hands = getHand(md, "B2", team3, 4, "S", "N", "N", true, 5) :: hands

    hands = getHand(md, "B7", team3, 4, "S", "N", "N", true, 5) :: hands
    hands = getHand(md, "B8", team2, 4, "S", "N", "N", true, 5) :: hands
    hands.reverse
  }

  def getTeamScore() = Map(team1 -> 3, team2 -> 1, team3 -> 1, team4 -> 3)

  def getPlayedMatch(dupid: Id.MatchDuplicate) = {
    var md = TestMatchDuplicate.create(dupid)
    val hands = TestMatchDuplicate.getHands(md)
    for (hand <- hands) {
      md = md.updateHand(hand)
    }
    md
  }

}
