package com.example.test.pages.duplicate

import com.example.test.pages.Page
import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.test.pages.PageBrowser._
import com.example.test.selenium.TestServer
import utils.logging.Logger
import com.example.test.util.HttpUtils
import com.example.data.BoardSet
import com.example.data.Movement
import java.net.URL
import com.example.test.pages.duplicate.ScoreboardPage.CompletedViewType
import com.example.test.pages.duplicate.ScoreboardPage.TableViewType
import com.example.data.bridge.PlayerPosition
import com.example.test.pages.GenericPage
import com.example.test.pages.Page.AnyPage
import com.example.data.bridge.North
import com.example.data.bridge.South
import com.example.data.bridge.East
import com.example.data.bridge.West
import com.example.data.util.Strings

object TableSelectNamesPage {

  val log = Logger[TableSelectNamesPage]

  def current( targetBoard: Option[String], scorekeeper: PlayerPosition)(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (dupid,tableid,roundid,targetboard) = findTableRoundId
    new TableSelectNamesPage(dupid,tableid,roundid,targetBoard,scorekeeper)
  }

  def urlFor( dupid: String, tableid: String, roundid: String, board: Option[String] ) =
    TableEnterScorekeeperPage.urlFor(dupid, tableid, roundid, board)

  def goto( dupid: String,
            tableid: String,
            roundid: String,
            board: Option[String] = None
          )( implicit
              webDriver: WebDriver,
              patienceConfig: PatienceConfig,
              pos: Position
          ) =
      TableEnterScorekeeperPage.goto(dupid, tableid, roundid, board)

  /**
   * Get the table id
   * currentUrl needs to be one of the following:
   *   duplicate/dupid/table/tableid/round/roundid/teams
   * @return (dupid, tableid,roundid)
   */
  def findTableRoundId(implicit webDriver: WebDriver, pos: Position) =
    TableSelectScorekeeperPage.findTableRoundId

  private def toPlayerNameText( loc: PlayerPosition ) = s"Player_${loc.pos}"

  val buttonOK = "OK"
  val buttonReset = "Reset"
  val buttonCancel = "Cancel"

  val buttonSwapLeft = "Swap_left"
  val buttonSwapRight = "Swap_right"
}

class TableSelectNamesPage( dupid: String,
                                 tableid: String,
                                 roundid: String,
                                 targetBoard: Option[String],
                                 scorekeeper: PlayerPosition
                               )( implicit
                                   webDriver: WebDriver,
                                   pageCreated: SourcePosition
                               ) extends Page[TableSelectNamesPage] {
  import TableSelectNamesPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor(dupid,tableid,roundid,targetBoard)

    findButtons( buttonOK, buttonReset, buttonCancel, buttonSwapLeft, buttonSwapRight )
    this
  }}

  def checkPlayers( north: String, south: String, east: String, west: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getPlayer(North) mustBe north
      getPlayer(South) mustBe south
      getPlayer(East) mustBe east
      getPlayer(West) mustBe west
    }
    this
  }

  /**
   * @param loc the location on the screen.  The scorekeeper's location is not valid.
   * @return the name of the player
   */
  def getPlayer( loc: PlayerPosition )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getElemById(toPlayerNameText(loc)).text
  }

  def getScorekeeperPos(implicit patienceConfig: PatienceConfig, pos: Position) = {
    PlayerPosition.fromDisplay( getElemByXPath("""//table/tbody/tr[3]/td[2]/span/b[1]""").text )
  }

  def clickSwapLeft(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonSwapLeft)
    this
  }

  def clickSwapRight(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonSwapRight)
    this
  }

  def clickOK(implicit patienceConfig: PatienceConfig, pos: Position): AnyPage = {
    clickButton(buttonOK)
    targetBoard match {
      case Some(boardid) => HandPage.current
      case None => new ScoreboardPage(Some(dupid), TableViewType(tableid,roundid) )
    }
  }

  def clickOKAndValidate(implicit patienceConfig: PatienceConfig, pos: Position): AnyPage = {
    clickButton(buttonOK)
    val ret: AnyPage = targetBoard match {
      case Some(boardid) => HandPage.current.validate
      case None => new ScoreboardPage(Some(dupid), TableViewType(tableid,roundid) ).validate
    }
    ret
  }

  def clickReset(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonReset)
    new TableEnterScorekeeperPage(dupid,tableid,roundid,targetBoard,None)
  }

  def isOKEnabled(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getButton(buttonOK).isEnabled
  }

  def verifyNamesAndSelect(
                   nsTeam: Int, ewTeam: Int,
                   north: String, south: String, east: String, west: String,
                   scorekeeper: PlayerPosition,
                   mustswap: Boolean
                 )( implicit
                     webDriver: WebDriver
                 ): AnyPage = {

    val left = scorekeeper.left
    val right = scorekeeper.right
    val top = scorekeeper.partner

    val playerL = left.player(north, south, east, west)
    val playerR = right.player(north, south, east, west)
    val playerT = top.player(north, south, east, west)
    val playerB = scorekeeper.player(north, south, east, west)

    val (lrteam,tbteam) = scorekeeper match {
      case North | South => (ewTeam,nsTeam)
      case East | West => (nsTeam,ewTeam)
    }

    val l = s"""${left.name} (Team ${lrteam})\n${playerL.trim}"""
    val r = s"""${right.name} (Team ${lrteam})\n${playerR.trim}"""
    val t = s"""${top.name} (Team ${tbteam})\n${playerT.trim}"""
    val b = s"""${scorekeeper.name} (Team ${tbteam})\n${playerB.trim}"""

    val cells = getElemsByXPath("""//div/table[2]/tbody/tr/td/span""").map(e => e.text)
    cells.size mustBe 4

    if (mustswap) {
      val lswap = s"""${left.name} (Team ${lrteam})\n${playerR}"""
      val rswap = s"""${right.name} (Team ${lrteam})\n${playerL}"""
      cells mustBe List(t,s"""${lswap}\nSwap ${Strings.arrowRightLeft}""",s"""${rswap}\nSwap ${Strings.arrowLeftRight}""",b)
      if (scorekeeper == North || scorekeeper == East) clickSwapLeft
      else clickSwapRight
      eventually {
        val cells = getElemsByXPath("""//div/table[2]/tbody/tr/td/span""").map(e => e.text)
        cells.size mustBe 4
        cells mustBe List(t,s"""${l}\nSwap ${Strings.arrowRightLeft}""",s"""${r}\nSwap ${Strings.arrowLeftRight}""",b)
      }
    } else {
      cells mustBe List(t,s"""${l}\nSwap ${Strings.arrowRightLeft}""",s"""${r}\nSwap ${Strings.arrowLeftRight}""",b)
    }
    clickOKAndValidate
  }

}