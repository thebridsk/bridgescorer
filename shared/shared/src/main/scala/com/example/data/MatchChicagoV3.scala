package com.example.data

import io.swagger.annotations._
import scala.annotation.meta._

import com.example.data.SystemTime.Timestamp
import com.example.data.bridge.PlayerPosition

/**
 * A match of chicago.
 * This can be either a 4 or 5 player chicago match with 4, 6, or 8 hands per round,
 * or a 5 player chicago match (quintet) with quick rotation (1 hand per round)
 *
 * @constructor
 * If playing quintet, then the gamesPerRound MUST be set when entering the names.
 *
 * @param id
 * @param players
 * @param rounds
 * @param gamesPerRound ==0 - unknown,
 *                      ==1 - quintet - quick rotation with 5 players
 *                      ==4, 6, or 8 - chicago with 4 or 5 players
 * @param simpleRotation ==true - simple rotation, last dealer swaps with sitting out
 *                       ==false - fair rotation, sitting out swaps with person that has not sat out yet,
 *                                                left of incoming stays put, right and partner of incoming swap
 * @param created
 * @param updated
 */
@ApiModel(value="MatchChicago", description = "A chicago match")
case class MatchChicagoV3(
    @(ApiModelProperty @field)(value="The chicago ID", required=true)
    id: String,
    @(ApiModelProperty @field)(value="The players", required=true)
    players: List[String],
    @(ApiModelProperty @field)(value="The rounds", required=true)
    rounds: List[Round],
    @(ApiModelProperty @field)(value="The number of games per round.", required=true)
    gamesPerRound: Int,
    @(ApiModelProperty @field)(value="Use simple rotation.", required=true)
    simpleRotation: Boolean,
    @(ApiModelProperty @field)(value="The creating date", required=true)
    created: Timestamp,
    @(ApiModelProperty @field)(value="The last update date", required=true)
    updated: Timestamp,
    @(ApiModelProperty @field)(value="best match in main store when importing, never written to store", required=false)
    bestMatch: Option[ChicagoBestMatch] = None
) extends VersionedInstance[ MatchChicago,MatchChicagoV3,String] {

  if (players.length < 4 || players.length > 5) {
    throw new IllegalArgumentException( "Must have 4 or 5 players")
  }

  def setId( newId: String, forCreate: Boolean, dontUpdateTime: Boolean = false ) = {
    if (dontUpdateTime) {
      copy(id=newId )
    } else {
      val time = SystemTime.currentTimeMillis()
      copy(id=newId, /* created=if (forCreate) time; else created, */ updated=time)
    }
  }

  def copyForCreate(id: Id.MatchDuplicate) = {
    val time = SystemTime.currentTimeMillis()
    val xrounds = rounds.map{ e => e.copyForCreate(e.id)}.toList
    copy( id=id, created=time, updated=time, rounds=xrounds )

  }

  def addRound( r: Round ) = {
    val n = copy(rounds= (r.copyForCreate(r.id)::(rounds.reverse)).reverse, updated=SystemTime.currentTimeMillis() )
    n
  }

  def modifyRound( r: Round ) = {
    if (rounds.isEmpty) addRound(r)
    else {
      var mod = false
      val rs = rounds.map { oldr =>
        if (oldr.id == r.id) {
          mod = true
          r
        }
        else {
          oldr
        }
      }
      val newr = if (mod) {
        rs
      } else {
        (r::(rounds.reverse)).reverse
      }
      copy( rounds=newr, updated=SystemTime.currentTimeMillis() )
    }
  }

  /**
   * Change the player names.
   * @param nplayers the new player names.  Must specify the same number as in the players field.
   */
  def setPlayers(nplayers: String* ): MatchChicagoV3 = {
    setPlayers( nplayers.toList )
  }

  /**
   * Change the player names.
   * @param nplayers the new player names.  Must specify the same number as in the players field.
   */
  def setPlayers(nplayers: List[String] ): MatchChicagoV3 = {
    if (nplayers.length != players.length) throw new IllegalArgumentException("Number of new player names must equal number of players")
    val map = (players zip nplayers).toMap
    val rs = rounds.map { r => r.modifyPlayers(map) }
    copy(players=nplayers,
         rounds=rs,
         updated=SystemTime.currentTimeMillis())
  }

  /**
   * Modify the player names according to the specified name map.
   * The timestamp is not changed.
   */
  def modifyPlayers( nameMap: Map[String,String] ) = {

    def getName( n: String ) = nameMap.get(n).getOrElse(n)

    val (rs, modified) = rounds.map { t =>
      t.modifyPlayersNoTime(nameMap) match {
        case Some(nt) => (nt, true)
        case None => (t, false)
      }
    }.foldLeft( (List[Round](),false) ) { (ac,v) =>
        (ac._1:::List(v._1), ac._2||v._2)
    }
    val (nps, pmodified) = players.map { p =>
      val np = getName(p)
      (np, !np.equals(p))
    }.foldLeft( (List[String](),false) ) { (ac,v) =>
        (ac._1:::List(v._1), ac._2||v._2)
    }
    if (modified || pmodified) {
      Some( copy( rounds=rs, players=nps ))
    } else {
      None
    }

  }

  @ApiModelProperty(hidden = true)
  def isConvertableToChicago5 = players.length==4 && rounds.length<2

  def playChicago5( extraPlayer: String ) = {
    if (!isConvertableToChicago5) throw new IllegalArgumentException("Number of players must be 4")
    val np = players:::List(extraPlayer)
    copy(players=np)
  }

  def setGamesPerRound( ngamesPerRound: Int ) = copy(gamesPerRound=ngamesPerRound, updated=SystemTime.currentTimeMillis())

  def addHandToLastRound( h: Hand ) = {
    val revrounds = rounds.reverse
    val last = revrounds.head
    val revbefore = revrounds.tail

    val nlast = last.addHand(h)
    val nrounds = (nlast::revbefore).reverse
    copy(rounds=nrounds, updated=SystemTime.currentTimeMillis())
  }

  /**
   * modify an existing hand
   * @param ir - the round, the round must exist. values are 0, 1, ...
   * @param ih - the hand, if the hand doesn't exist, then it will addHandToLastRound. values are 0, 1, ...
   * @param h - the new hand
   */
  def modifyHand( ir: Int, ih: Int, h: Hand ) = {
    val rs = rounds.toArray
    val round = rs(ir)
    val hs = round.hands.toArray
    if (ih < hs.length) {
      hs(ih) = h
      rs(ir) = round.copy(hands=hs.toList, updated=SystemTime.currentTimeMillis())
      copy(rounds=rs.toList, updated=SystemTime.currentTimeMillis())
    } else {
      addHandToLastRound(h)
    }
  }

  /**
   * Set the Id of this match
   * @param id the new ID of the match
   */
  def setId( id: String ) = {
    copy(id = id, updated=SystemTime.currentTimeMillis())
  }

  /**
   * Is this a quintet match
   */
  @ApiModelProperty(hidden = true)
  def isQuintet() = {
    gamesPerRound == 1
  }

  /**
   * Start a match of quintet.
   * This can only be done if gamesPerRound is still 0 AND no rounds have been started.
   */
  @ApiModelProperty(hidden = true)
  def setQuintet( simple: Boolean ) = {
    if (gamesPerRound != 0 || !rounds.isEmpty) this
    setGamesPerRound(1).copy(simpleRotation=simple)
  }

  def convertToCurrentVersion(): MatchChicago = this

  def readyForWrite() = copy( bestMatch=None )

  def addBestMatch( bm: ChicagoBestMatch ) = copy( bestMatch = Option(bm))

}

object MatchChicagoV3 {
  def apply( id: String,
             players: List[String],
             rounds: List[Round],
             gamesPerRound: Int,
             simpleRotation: Boolean) = {
    val time = SystemTime.currentTimeMillis()
    new MatchChicagoV3(id,players,rounds,gamesPerRound,simpleRotation,time,time)
  }
}

@ApiModel(description = "The best match in the main store")
case class ChicagoBestMatch(
    @(ApiModelProperty @field)(value="How similar the matches are", required=true)
    sameness: Double,
    @(ApiModelProperty @field)(value="The ID of the MatchChicago in the main store that is the best match, none if no match", required=true)
    id: Option[Id.MatchChicago],
    @(ApiModelProperty @field)(value="The fields that are different", required=true)
    differences: Option[List[String]]
) {

  def determineDifferences( l: List[String] ) = {
    val list = l.map { s =>
      val i = s.lastIndexOf(".")
      if (i<0) ("",s)
      else ( s.substring(0, i), s.substring(i+1))
    }.foldLeft(Map[String,List[String]]()) { ( ac, v ) =>
      val (prefix, key) = v
      val cur = ac.get(prefix).getOrElse(List())
      val next = key::cur
      val r = ac + (prefix -> next)
      r
    }

//    list.map{ s =>
//      val parts = s.split('.')
//      if (parts.isEmpty) s
//      else parts.head
//    }.distinct
    list.map { entry =>
      val (prefix, vkeys) = entry
      val keys = vkeys.sorted
      if (prefix == "") s"""${keys.mkString(" ")}"""
      else s"""${prefix} ${keys.mkString(" ")}"""
    }.toList.sorted
  }

  def htmlTitle = {
    differences.map { l =>
      if (l.isEmpty) "Same"
      else determineDifferences(l).mkString("Differences:\n","\n","")
    }
  }
}

object ChicagoBestMatch {

  def noMatch = new ChicagoBestMatch( -1, None, None )

  def apply( id: String, diff: Difference ) = {
    new ChicagoBestMatch( diff.percentSame, Some(id), Some(diff.differences) )
  }
}
