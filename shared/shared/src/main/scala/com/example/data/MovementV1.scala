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
}

case class BoardPlayed( board: Int, table: Int, round: Int, ns: Int, ew: Int )

case class HandInTable( table: Int, round: Int, ns: Int, ew: Int, boards: List[Int] )
