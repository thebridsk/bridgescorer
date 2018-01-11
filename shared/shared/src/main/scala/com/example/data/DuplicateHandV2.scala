package com.example.data

import com.example.data.bridge.DuplicateBridge
import com.example.data.SystemTime.Timestamp
import com.example.data.bridge.DuplicateBridge.DuplicateScore

import io.swagger.annotations._
import scala.annotation.meta._

@ApiModel(value="DuplicateHand", description = "A hand from a duplicate match")
case class DuplicateHandV2 private(
    @(ApiModelProperty @field)(value="The played hand.  Length of 0 indicates not played, length of 1 indicates played.", required=true)
    played: List[Hand],
    @(ApiModelProperty @field)(value="The table id of where the hand is played", required=true)
    table: String,
    @(ApiModelProperty @field)(value="The round the hand is played in", required=true)
    round: Int,
    @(ApiModelProperty @field)(value="The board id", required=true)
    board: Id.DuplicateBoard,
    @(ApiModelProperty @field)(value="The team id of the team playing NS.  This is also the id of the DuplicateHand", required=true)
    nsTeam: Id.Team,
    @(ApiModelProperty @field)(value="true if player 1 of the NS team is the north player", required=true)
    nIsPlayer1: Boolean,
    @(ApiModelProperty @field)(value="The team id of the team playing EW", required=true)
    ewTeam: Id.Team,
    @(ApiModelProperty @field)(value="true if player 1 of the EW team is the east player", required=true)
    eIsPlayer1: Boolean,
    @(ApiModelProperty @field)(value="when the duplicate hand was created", required=true)
    created: Timestamp,
    @(ApiModelProperty @field)(value="when the duplicate hand was last updated", required=true)
    updated: Timestamp ) {

  def equalsIgnoreModifyTime( other: DuplicateHandV2 ) = table==other.table &&
                                                       round==other.round &&
                                                       board==other.board &&
                                                       nsTeam==other.nsTeam &&
                                                       nIsPlayer1==other.nIsPlayer1 &&
                                                       ewTeam==other.ewTeam &&
                                                       eIsPlayer1==other.eIsPlayer1 &&
                                                       handEquals(other)

  def hand: Option[Hand] = played.headOption

  def wasPlayed = !played.isEmpty

  def handEquals( other: DuplicateHandV2 ) = {
    hand match {
      case Some(h) =>
        other.hand match {
          case Some(oh) =>
            h.equalsIgnoreModifyTime(oh)
          case _ =>
            false
        }
      case _ =>
        other.hand match {
          case Some(oh) =>
            false
          case _ =>
            true
        }
    }
  }

  def setId( newId: Id.DuplicateHand, forCreate: Boolean ) = {
      val time = SystemTime.currentTimeMillis()
      copy(nsTeam=newId, created=if (forCreate) time; else created, updated=time)
    }

  def isNSTeam( team: Id.Team ) = team==nsTeam
  def isEWTeam( team: Id.Team ) = team==ewTeam

  def isTeam( team: Id.Team ) = isNSTeam(team) || isEWTeam(team)

  def score = hand match {
    case Some(h) => DuplicateBridge.ScoreHand(h).score
    case _ => DuplicateScore(0,0)
  }

  def id: Id.DuplicateHand = nsTeam

  def copyForCreate(id: Id.DuplicateHand) = {
    val time = SystemTime.currentTimeMillis()
    copy( nsTeam=id, created=time, updated=time, played=getListFromHand(hand) )
  }

  private def getListFromHand( original: Option[Hand] ): List[Hand] = original match {
    case Some(h) => List( h.copyForCreate(h.id) )
    case _ => List()
  }

  def updateHand( newhand: Hand ) = copy( played=List(newhand), updated=SystemTime.currentTimeMillis() )

  def setPlayer1North( flag: Boolean ) = copy( nIsPlayer1=flag, updated=SystemTime.currentTimeMillis() )
  def setPlayer1East( flag: Boolean ) = copy( eIsPlayer1=flag, updated=SystemTime.currentTimeMillis() )

  @ApiModelProperty(hidden = true)
  def convertToCurrentVersion() = {
    DuplicateHandV2( hand.toList,
                     table,
                     round,
                     board,
                     nsTeam,
                     nIsPlayer1,
                     ewTeam,
                     eIsPlayer1,
                     created,
                     updated )
  }
}

object DuplicateHandV2 {
  def create( hand: Option[Hand],
             table: String,
             round: Int,
             board: Id.DuplicateBoard,
             nsTeam: Id.Team,
             ewTeam: Id.Team) = {
    val time = SystemTime.currentTimeMillis()
    val nh = hand.toList
    DuplicateHandV2(nh,table,round,board,nsTeam,true,ewTeam,true,time,time)
  }

  def create( hand: Hand,
             table: String,
             round: Int,
             board: Id.DuplicateBoard,
             nsTeam: Id.Team,
             ewTeam: Id.Team) = {
    val time = SystemTime.currentTimeMillis()
    DuplicateHandV2(List(hand),table,round,board,nsTeam,true,ewTeam,true,time,time)
  }

  def create( table: String,
             round: Int,
             board: Id.DuplicateBoard,
             nsTeam: Id.Team,
             ewTeam: Id.Team) = {
    val time = SystemTime.currentTimeMillis()
    DuplicateHandV2(List(),table,round,board,nsTeam,true,ewTeam,true,time,time)
  }

  def sort(l: Hand,r: Hand) = l.id<r.id

  def apply(
    played: List[Hand],
    table: String,
    round: Int,
    board: Id.DuplicateBoard,
    nsTeam: Id.Team,
    nIsPlayer1: Boolean,
    ewTeam: Id.Team,
    eIsPlayer1: Boolean,
    created: Timestamp,
    updated: Timestamp ) = {
    new DuplicateHandV2(played.sortWith(sort),table,round,board,nsTeam,nIsPlayer1,ewTeam,eIsPlayer1,created,updated)
  }
}
