package com.example.data.sample

import com.example.data.Id
import com.example.data.MatchDuplicate
import com.example.data.Team
import com.example.data.Board
import com.example.data.DuplicateHand
import com.example.data.Hand

object TestMatchDuplicate {

  def getHand( dm: MatchDuplicate, boardid: Id.DuplicateBoard, handid: Id.DuplicateHand,
               contractTricks: Int,
               contractSuit: String,
               contractDoubled: String,
               declarer: String,
               madeContract: Boolean,
               tricks: Int
             ) = {
    val b = dm.getBoard(boardid).get
    val dh = b.getHand(handid).get
    dh.updateHand(Hand.create(dh.id, contractTricks, contractSuit,contractDoubled, declarer, b.nsVul, b.ewVul, madeContract, tricks ))
  }

  def teams() = Map("T1"->Team.create("T1","Nancy","Norman"),
                    "T2"->Team.create("T2","Ellen","Edward"),
                    "T3"->Team.create("T3","Susan","Sam"),
                    "T4"->Team.create("T4","Wilma","Wayne"))

  def create( id: Id.MatchDuplicate ): MatchDuplicate = {
    val ts: Map[Id.Team, Team] = teams()

    val boards = scala.collection.mutable.Map[Id.DuplicateBoard, Board]()

    def addBoard( board: Board ) = boards += (board.id -> board)

    addBoard( Board.create( "B1", false, false, "N", List() ) )
    addBoard( Board.create( "B2", true,  false, "E", List() ) )
    addBoard( Board.create( "B7", true,  true,  "S", List() ) )
    addBoard( Board.create( "B8", false, false, "W", List() ) )

    def addHand( hand: DuplicateHand ) = {
      val board = boards.get(hand.board).get
      val hands = hand::board.hands
      boards += (hand.board->board.updateHand(hand))
    }

    addHand( DuplicateHand.create("1",1,"B1","T1","T2") )
    addHand( DuplicateHand.create("1",1,"B2","T1","T2") )
    addHand( DuplicateHand.create("2",2,"B1","T3","T4") )
    addHand( DuplicateHand.create("2",2,"B2","T3","T4") )
    addHand( DuplicateHand.create("1",3,"B7","T3","T1") )
    addHand( DuplicateHand.create("1",3,"B8","T3","T1") )
    addHand( DuplicateHand.create("2",4,"B7","T2","T4") )
    addHand( DuplicateHand.create("2",4,"B8","T2","T4") )

    MatchDuplicate( id, ts.values.toList, boards.values.toList, "", "", 0, 0 )
  }

  def getHands( md: MatchDuplicate ) = {
    var hands: List[DuplicateHand] = Nil
    hands = getHand(md, "B1", "T1", 3, "N", "N", "N", true, 5 ) :: hands
    hands = getHand(md, "B2", "T1", 4, "S", "N", "N", true, 5 ) :: hands
    hands = getHand(md, "B1", "T3", 4, "S", "N", "N", true, 5 ) :: hands
    hands = getHand(md, "B2", "T3", 4, "S", "N", "N", true, 5 ) :: hands

    hands = getHand(md, "B7", "T3", 4, "S", "N", "N", true, 5 ) :: hands
    hands = getHand(md, "B8", "T2", 4, "S", "N", "N", true, 5 ) :: hands
    hands.reverse
  }

  def getTeamScore() = Map( "T1"->3,"T2"->1,"T3"->1,"T4"->3 )

  def getPlayedMatch( dupid: Id.MatchDuplicate) = {
    var md = TestMatchDuplicate.create(dupid)
    val hands = TestMatchDuplicate.getHands(md)
    for (hand <- hands) {
      md = md.updateHand(hand)
    }
    md
  }

}
