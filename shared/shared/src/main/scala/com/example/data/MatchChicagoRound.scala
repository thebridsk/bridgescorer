package com.example.data

import io.swagger.annotations._
import scala.annotation.meta._

import com.example.data.SystemTime.Timestamp
import com.example.data.bridge.PlayerPosition

/**
 * @author werewolf
 */
@ApiModel(description = "A round in a chicago match")
case class Round(
    @(ApiModelProperty @field)(value="The round ID", required=true)
    id: String,
    @(ApiModelProperty @field)(value="The north player for the round", required=true)
    north: String,
    @(ApiModelProperty @field)(value="The south player for the round", required=true)
    south: String,
    @(ApiModelProperty @field)(value="The east player for the round", required=true)
    east: String,
    @(ApiModelProperty @field)(value="The west player for the round", required=true)
    west: String,
    @(ApiModelProperty @field)(value="The first dealer, N, S, E, W", required=true)
    dealerFirstRound: String,
    @(ApiModelProperty @field)(value="The played hands in the round", required=true)
    hands: List[Hand],
    @(ApiModelProperty @field)(value="when the duplicate hand was created", required=true)
    created: Timestamp,
    @(ApiModelProperty @field)(value="when the duplicate hand was last updated", required=true)
    updated: Timestamp ) {

  def setId( newId: String, forCreate: Boolean ) = {
      val time = SystemTime.currentTimeMillis()
      copy(id=newId, created=if (forCreate) time; else created, updated=time)
    }

  def copyForCreate(id: String) = {
    val time = SystemTime.currentTimeMillis()
    val xhands = hands.map{ e => e.copyForCreate(e.id) }.toList
    copy( id=id, created=time, updated=time, hands=xhands )

  }

  def addHand( h: Hand ) = copy(hands=(h::(hands.reverse)).reverse, updated=SystemTime.currentTimeMillis())

  def partnerOf( p: String ) = p match {
    case `north` => south
    case `south` => north
    case `east` => west
    case `west` => east
    case _ => null
  }

  def modifyPlayersNoTime( map: Map[String,String]) = {
    val n = map.get(north).getOrElse(north)
    val s = map.get(south).getOrElse(south)
    val e = map.get(east).getOrElse(east)
    val w = map.get(west).getOrElse(west)
    if (n.equals(north) && s.equals(south) && e.equals(east) && w.equals(west)) {
      None
    } else {
      Some(copy( north=n, south=s, east=e, west=w ))
    }

  }

  def modifyPlayers( map: Map[String,String]) = {
    val n = map.get(north).getOrElse(north)
    val s = map.get(south).getOrElse(south)
    val e = map.get(east).getOrElse(east)
    val w = map.get(west).getOrElse(west)
    copy( north=n, south=s, east=e, west=w, updated=SystemTime.currentTimeMillis() )
  }

  def players() = north::south::east::west::Nil
}

object Round {
  def create(id: String,
             north: String,
             south: String,
             east: String,
             west: String,
             dealerFirstRound: String,
             hands: List[Hand] ) =
     new Round(id,north,south,east,west,dealerFirstRound,hands,0,0).copyForCreate(id)

}
