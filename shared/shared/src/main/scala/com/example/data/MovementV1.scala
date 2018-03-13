package com.example.data

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * <pre><code>
 * {
 *   "name": "Armonk2Tables",
 *   "short": "Armonk 2 Tables",
 *   "description": "2 tables, 18 hands, used by Armonk bridge group",
 *   "hands": [
 *     {
 *       "table": "1",
 *       "round": "1",
 *       "NS": "1",
 *       "EW": "2",
 *       "board": "1"
 *     },
 *     ...
 *   ]
 * }
 * </code></pre>
 */
@ApiModel(value="Movement", description = "A movements for a duplicate bridge match")
case class MovementV1( name: String, short: String, description: String, numberTeams: Int, hands: List[HandInTable] ) extends VersionedInstance[MovementV1,MovementV1,String] {

  def id = name

  def setId( newId: String, forCreate: Boolean, dontUpdateTime: Boolean = false ) = {
    copy( name=newId )
  }

  def convertToCurrentVersion() = this

  def wherePlayed( board: Int ): List[BoardPlayed] = {
    hands.flatMap { h =>
      if (h.boards.contains(board)) BoardPlayed( board, h.table, h.round, h.ns, h.ew )::Nil
      else Nil
    }
  }

  @ApiModelProperty(hidden = true)
  def getBoards = {
    hands.flatMap( h => h.boards ).distinct.sorted
  }


  @ApiModelProperty(hidden = true)
  def getRoundForAllTables( round: Int ) = {
    hands.filter { r =>
      r.round == round
    }.toList
  }

  @ApiModelProperty(hidden = true)
  def allRounds = {
    hands.map( r => r.round ).distinct
  }

  @ApiModelProperty(hidden = true)
  def matchHasRelay = {
    allRounds.find { ir =>
      val all = getRoundForAllTables(ir).flatMap( r => r.boards )
      val distinct = all.distinct
      all.length != distinct.length
    }.isDefined
  }

  /**
   * @returns table IDs
   */
  @ApiModelProperty(hidden = true)
  def tableRoundRelay( itable: Int, iround: Int ) = {
    val allRounds = getRoundForAllTables(iround)
    val otherRounds = allRounds.filter( r => r.table != itable )
    val otherBoards = otherRounds.flatMap( r => r.boards )
    allRounds.find( r => r.table == itable ).map { table =>
      val relays = table.boards.flatMap { bid =>
        otherRounds.flatMap { r =>
          r.boards.find( bs => bs == bid ).map( bs => r.table )
        }
      }.distinct.sorted
      relays
    }.getOrElse(Nil)
  }

}

case class BoardPlayed( board: Int, table: Int, round: Int, ns: Int, ew: Int )

case class HandInTable( table: Int, round: Int, ns: Int, ew: Int, boards: List[Int] )
