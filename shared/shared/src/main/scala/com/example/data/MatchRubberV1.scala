package com.example.data

import io.swagger.annotations._
import scala.annotation.meta._

import com.example.data.SystemTime.Timestamp

/**
 * @author werewolf
 */
@ApiModel(value="MatchRubber", description = "A rubber bridge match")
case class MatchRubberV1(
    @(ApiModelProperty @field)(value="The round ID", required=true)
    id: String,
    @(ApiModelProperty @field)(value="The north player", required=true)
    north: String,
    @(ApiModelProperty @field)(value="The south player", required=true)
    south: String,
    @(ApiModelProperty @field)(value="The east player", required=true)
    east: String,
    @(ApiModelProperty @field)(value="The west player", required=true)
    west: String,
    @(ApiModelProperty @field)(value="The first dealer, N, S, E, W", required=true)
    dealerFirstHand: String,
    @(ApiModelProperty @field)(value="The played hands in the round", required=true)
    hands: List[RubberHand],
    @(ApiModelProperty @field)(value="when the match rubber was created", required=true)
    created: Timestamp,
    @(ApiModelProperty @field)(value="when the match rubber was last updated", required=true)
    updated: Timestamp,
    @(ApiModelProperty @field)(value="best match in main store when importing, never written to store", required=false)
    bestMatch: Option[RubberBestMatch] = None
) extends VersionedInstance[MatchRubberV1,MatchRubberV1,String] {

  def equalsIgnoreModifyTime( other: MatchRubberV1 ) = {
    other.id == id &&
    other.north == north &&
    other.south == south &&
    other.east == east &&
    other.west == west &&
    other.dealerFirstHand == dealerFirstHand &&
    (other.hands zip hands).map( e => e._1.equalsIgnoreModifyTime(e._2)).find(b => !b).isEmpty

  }

  def setId( newId: String, forCreate: Boolean, dontUpdateTime: Boolean = false ) = {
    if (dontUpdateTime) {
      copy(id=newId )
    } else {
      val time = SystemTime.currentTimeMillis()
      copy(id=newId, /* created=if (forCreate) time; else created, */ updated=time)
    }
  }

  def copyForCreate(id: String) = {
    val time = SystemTime.currentTimeMillis()
    val xhands = hands.map{ e => e.copyForCreate(e.id) }.toList
    copy( id=id, created=time, updated=time, hands=xhands )

  }

  def setPlayers( north: String, south: String, east: String, west: String ) =
    copy( north=north, south=south, east=east, west=west, updated=SystemTime.currentTimeMillis())

  def gotAllPlayers() = {
    north != null && north != "" && south != null && south != "" && east != null && east != "" && west != null && west != ""
  }

  /**
   * @param pos the first dealer, N, S, E, W
   */
  def setFirstDealer( pos: String ) = copy( dealerFirstHand=pos, updated=SystemTime.currentTimeMillis())

  /**
   * Will set the id of the hand to the next ID.
   */
  def addHand( h: RubberHand ) = {
    val hh = h.setId(hands.length.toString(), false)
    copy(hands=(hh::(hands.reverse)).reverse, updated=SystemTime.currentTimeMillis())
  }

  def getHand( id: String ) = {
    hands.find { h => h.id == id }
  }

  def partnerOf( p: String ) = p match {
    case `north` => south
    case `south` => north
    case `east` => west
    case `west` => east
    case _ => null
  }

  /**
   * The hand with the same id as the specified hand will get updated
   * If the id is not in hands, then hands will not be updated, but the updated time will be changed.
   */
  def modifyHand( h: RubberHand ) = {
    val hs = hands.map { hh => if (hh.id == h.id) h else hh }
    copy( hands=hs, updated=SystemTime.currentTimeMillis())
  }

  /**
   * Modify the player names according to the specified name map.
   * The timestamp is not changed.
   */
  def modifyPlayers( nameMap: Map[String,String] ) = {

    def getName( p: String ) = nameMap.get(p).getOrElse(p)

    val n=getName(north)
    val s=getName(south)
    val e=getName(east)
    val w=getName(west)

    if (n.equals(north) && s.equals(south) && e.equals(east) && w.equals(west)) {
      None
    } else {
      Some(copy( north=n, south=s, east=e, west=w ))
    }
  }

  def convertToCurrentVersion() = this

  def readyForWrite() = copy( bestMatch=None )

  def addBestMatch( bm: RubberBestMatch ) = copy( bestMatch = Option(bm))
}

object MatchRubberV1 {
  def apply(
    id: String,
    north: String,
    south: String,
    east: String,
    west: String,
    dealerFirstHand: String,
    hands: List[RubberHand] ) = {
    val time = SystemTime.currentTimeMillis()
    new MatchRubberV1(id,north,south,east,west,dealerFirstHand,hands,time,time)
  }
}

@ApiModel(description = "The best match in the main store")
case class RubberBestMatch(
    @(ApiModelProperty @field)(value="How similar the matches are", required=true)
    sameness: Double,
    @(ApiModelProperty @field)(value="The ID of the MatchRubber in the main store that is the best match, none if no match", required=true)
    id: Option[String],
    @(ApiModelProperty @field)(value="The fields that are different", required=true)
    differences: Option[List[String]]
)

object RubberBestMatch {

  def noMatch = new RubberBestMatch( -1, None, None )

  def apply( id: String, diff: Difference ) = {
    new RubberBestMatch( diff.percentSame, Some(id), Some(diff.differences) )
  }
}
