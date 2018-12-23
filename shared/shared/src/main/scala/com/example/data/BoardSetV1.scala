package com.example.data

import io.swagger.annotations.ApiModel

/**
 * Contains a set of boards.  For each board there is the following information:
 * <dl>
 * <dt>id
 * <dd>the board number as a string
 * <dt>NSVul
 * <dd>true if NS is vulnerable
 * <dt>EWVul
 * <dd>true if EW is vulnerable
 * <dt>Dealear
 * <dd>The dealer, valid values are: "N", "E", "S", "W"
 * </dl>
 * <pre><code>
 * {
 *   "name": "ArmonkBoards",
 *   "short": "Armonk Boards",
 *   "description": "The boards used by Armonk bridge group",
 *   "boards": [
 *     {
 *       "id": "1",
 *       "NSVul": "false",
 *       "EWVul": "false",
 *       "Dealer": "N"
 *     },
 *     ...
 *   ]
 * }
 * </code></pre>
 *
 */
@ApiModel(value="BoardSet", description = "A set of boards for duplicate bridge")
case class BoardSetV1 ( name: String, short: String, description: String, boards: List[BoardInSet] ) extends VersionedInstance[BoardSetV1,BoardSetV1,String] {

  def id = name

  def setId( newId: String, forCreate: Boolean, dontUpdateTime: Boolean = false ) = {
    copy( name=newId )
  }

  def convertToCurrentVersion() = this

  def readyForWrite() = this

}

case class BoardInSet( id: Int, nsVul: Boolean, ewVul: Boolean, dealer: String )
