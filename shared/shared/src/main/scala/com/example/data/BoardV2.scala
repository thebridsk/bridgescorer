package com.example.data

import com.example.data.bridge.PlayerPosition
import com.example.data.SystemTime.Timestamp

import io.swagger.annotations._
import scala.annotation.meta._

/**
 * @author werewolf
 *
 * @param id the board number
 * @param nsVul true if NS is vulnerable
 * @param ewVul true if EW is vulnerable
 * @param dealer the dealer on the board
 * @param hands map nsTeam -> DuplicateHand
 */
@ApiModel(value="Board", description = "A board from a duplicate match")
case class BoardV2 private(
    @(ApiModelProperty @field)(value="The ID of the board", required=true)
    id: Id.DuplicateBoard,
    @(ApiModelProperty @field)(value="True if NS is vulnerable on the board", required=true)
    nsVul: Boolean,
    @(ApiModelProperty @field)(value="True if EW is vulnerable on the board", required=true)
    ewVul: Boolean,
    @(ApiModelProperty @field)(value="The dealer for the board", required=true)
    dealer: String,
    @(ApiModelProperty @field)(value="The duplicate hands for the board", required=true)
    hands: List[DuplicateHandV2],
    @(ApiModelProperty @field)(value="when the duplicate hand was created", required=true)
    created: Timestamp,
    @(ApiModelProperty @field)(value="when the duplicate hand was last updated", required=true)
    updated: Timestamp ) {

  def equalsIgnoreModifyTime( other: BoardV2 ) = id==other.id &&
                                               nsVul==other.nsVul &&
                                               ewVul==other.ewVul &&
                                               dealer==other.dealer && equalsInHands(other)

  def equalsInHands( other: BoardV2, throwit: Boolean = false ) = {
    if (hands.length == other.hands.length) {
      hands.find { t1 =>
        // this function must return true if t1 is NOT in other.team
        val rc = other.hands.find { t2 => t1.id == t2.id && t1.equalsIgnoreModifyTime(t2) }.isEmpty
        if (!rc&&throwit) throw new Exception("MatchDuplicateV3 other did not have hand equal to: "+t1)
        rc
      }.isEmpty
    } else {
      if (throwit) throw new Exception("MatchDuplicateV3 hands don't have same key: "+hands.map{t=>t.id}+" "+other.hands.map{t=>t.id})
      false
    }
  }

  def setId( newId: Id.DuplicateBoard, forCreate: Boolean ) = {
      val time = SystemTime.currentTimeMillis()
      copy(id=newId, created=if (forCreate) time; else created, updated=time)
    }

  def timesPlayed() = hands.filter(dh => dh.wasPlayed).size

  def handPlayedByTeam( team: Id.Team ) = hands.collectFirst { case hand: DuplicateHandV2 if hand.isTeam(team) => hand}

  def wasPlayedByTeam( team: Id.Team ) = !handPlayedByTeam(team).isEmpty

  def handTeamPlayNS( team: Id.Team ) = hands.collectFirst{case hand: DuplicateHandV2 if hand.isNSTeam(team) => hand}

  def didTeamPlayNS( team: Id.Team ) = !handTeamPlayNS(team).isEmpty

  def handTeamPlayEW( team: Id.Team ) = hands.collectFirst{case hand: DuplicateHandV2 if hand.isEWTeam(team) => hand}

  def didTeamPlayEW( team: Id.Team ) = !handTeamPlayEW(team).isEmpty

  def teamScore( team: Id.Team ) =
    handPlayedByTeam( team ) match {
    case Some(teamHand) =>
      def getNSTeam( hand: DuplicateHandV2) = hand.nsTeam
      def getEWTeam( hand: DuplicateHandV2) = hand.ewTeam
      def getNSScore( hand: DuplicateHandV2) = hand.score.ns
      def getEWScore( hand: DuplicateHandV2) = hand.score.ew
      val teamPlayedNS = teamHand.nsTeam == team
      teamScorePrivate( team,
                        if (teamPlayedNS) getNSScore(teamHand); else getEWScore(teamHand),
                        if (teamPlayedNS) getNSScore _; else getEWScore _,
                        if (teamPlayedNS) getNSTeam _; else getEWTeam _ )

    case _ => 0
    }

  private[this] def teamScorePrivate( team: Id.Team,
                                      score: Int,
                                      getScoreFromHand: (DuplicateHandV2)=>Int,
                                      getTeam: (DuplicateHandV2)=>Id.Team ) = {
    hands.filter( hand => getTeam(hand) != team ).map( hand => {
      val otherscore = getScoreFromHand(hand)
      if (score == otherscore) 0.5f
      else if (score > otherscore) 1
      else 0
    }).reduce( _ + _ )
  }

  def copyForCreate(id: Id.DuplicateBoard) = {
    val time = SystemTime.currentTimeMillis()
    val xhands = hands.map( e=> e.copyForCreate(e.id) )
    copy( id=id, created=time, updated=time, hands=xhands.sortWith(BoardV2.sort) )
  }

  def updateHand( hand: DuplicateHandV2 ): BoardV2 = {
    val nb = hands.map{b=> if (b.id==hand.id) (true,hand) else (false,b) }
    val nb1 = nb.foldLeft((false,List[DuplicateHandV2]()))((ag,b) => ( ag._1||b._1, b._2::ag._2))
    val nb2 = if (nb1._1) nb1._2 else hand::nb1._2
    copy( hands=nb2.sortWith(BoardV2.sort), updated=SystemTime.currentTimeMillis())
  }
  def updateHand( handId: Id.DuplicateHand, hand: Hand ): BoardV2 = getHand(handId) match {
    case Some(dh) => updateHand( dh.updateHand(hand) )
    case None => throw new IndexOutOfBoundsException("Hand "+handId+" not found")
  }

  def setHands( nhands: List[DuplicateHandV2] ) = {
    copy( hands = nhands, updated=SystemTime.currentTimeMillis() )
  }

  def deleteHand( handId: Id.DuplicateHand ) = {
    val nb = hands.filter(h=>h.id!=handId)
    copy( hands = nb, updated=SystemTime.currentTimeMillis() )

  }

  @ApiModelProperty(hidden = true)
  def getHand( handId: Id.DuplicateHand ) = {
    hands.find(h=>h.id==handId)
  }

  @ApiModelProperty(hidden = true)
  def getBoardInSet() = BoardInSet(Id.boardIdToBoardNumber(id).toInt,nsVul,ewVul,dealer)

  @ApiModelProperty(hidden = true)
  def convertToCurrentVersion() = BoardV2( id,
                                           nsVul,
                                           ewVul,
                                           dealer,
                                           hands.map(h=>h.convertToCurrentVersion()),
                                           created,
                                           updated
                                          )

}

object BoardV2 {
  def create( id: Id.DuplicateBoard, nsVul: Boolean, ewVul: Boolean, dealer: String, hands: List[DuplicateHandV2] ) = {
    val time = SystemTime.currentTimeMillis()
    BoardV2(id,nsVul,ewVul,dealer,hands,time,time)
  }

  def sort(l: DuplicateHandV2,r: DuplicateHandV2) = Id.idComparer(l.id, r.id)<0

  def apply(
    id: Id.DuplicateBoard,
    nsVul: Boolean,
    ewVul: Boolean,
    dealer: String,
    hands: List[DuplicateHandV2],
    created: Timestamp,
    updated: Timestamp ) = {
      new BoardV2(id,nsVul,ewVul,dealer,hands.sortWith(sort),created,updated)
  }

}
