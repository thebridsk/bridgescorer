package com.example.pages.duplicate

import com.example.pages.Page
import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.pages.PageBrowser._
import com.example.test.selenium.TestServer
import utils.logging.Logger
import com.example.test.util.HttpUtils
import com.example.data.BoardSet
import com.example.data.Movement
import java.net.URL
import com.example.pages.duplicate.ScoreboardPage.CompletedViewType
import com.example.pages.duplicate.ScoreboardPage.TableViewType
import com.example.data.bridge.PlayerPosition
import com.example.pages.GenericPage
import com.example.pages.duplicate.TablePage.EnterNames
import com.example.data.bridge._

object TableSelectScorekeeperPage {

  val log = Logger[TableSelectScorekeeperPage]

  def current(scorekeeper: Option[PlayerPosition] = None)(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (dupid,tableid,roundid,targetboard) = findTableRoundId
    new TableEnterScorekeeperPage(dupid,tableid,roundid,targetboard,scorekeeper)
  }

  def urlFor( dupid: String, tableid: String, roundid: String, board: Option[String] ) = {
    val b = board.map(bb => s"boards/B${bb}/").getOrElse("")
    TestServer.getAppPageUrl( s"duplicate/${dupid}/table/${tableid}/round/${roundid}/${b}teams" )
  }

  def goto( dupid: String,
            tableid: String,
            roundid: String,
            board: Option[String] = None,
            scorekeeper: Option[PlayerPosition] = None
          )( implicit
              webDriver: WebDriver,
              patienceConfig: PatienceConfig,
              pos: Position
          ) = {
    go to urlFor(dupid,tableid,roundid, board)
    new TableSelectScorekeeperPage(dupid,tableid,roundid,board,scorekeeper)
  }

  /**
   * Get the table id
   * currentUrl needs to be one of the following:
   *   duplicate/dupid/table/tableid/round/roundid/teams
   * @return (dupid, tableid,roundid)
   */
  def findTableRoundId(implicit webDriver: WebDriver, pos: Position): (String,String,String,Option[String]) =
    TableEnterScorekeeperPage.findTableRoundId

  private def toNameButton( name: String ) = s"P_${name}"

  private def toScorekeeperButton( loc: PlayerPosition ) = s"SK_${loc.pos}"

  private val scorekeeperButtons = (North::South::East::West::Nil).map(p=>(toScorekeeperButton(p),p)).toMap

  val buttonOK = "OK"
  val buttonReset = "Reset"
  val buttonCancel = "Cancel"

}

class TableSelectScorekeeperPage( dupid: String,
                                 tableid: String,
                                 roundid: String,
                                 targetBoard: Option[String],
                                 scorekeeper: Option[PlayerPosition] = None
                               )( implicit
                                   webDriver: WebDriver,
                                   pageCreated: SourcePosition
                               ) extends Page[TableSelectScorekeeperPage] {
  import TableSelectScorekeeperPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor(dupid,tableid,roundid,targetBoard)

    findButtons( buttonOK, buttonCancel, buttonReset )
    this
  }}

  /**
   * Get the selected made or down.
   * @return None if nothing is selected, Some(n) if n is selected
   * @throws TestFailedException if more than one is selected
   */
  def getSelectedScorekeeper(implicit patienceConfig: PatienceConfig, pos: Position) = {

    val x = s"""//button[starts-with( @id, 'P_' ) and contains(concat(' ', @class, ' '), ' baseButtonSelected ')]"""

    val elems = findElemsByXPath(x)

    if (elems.isEmpty) None
    else {
      elems.size mustBe 1
      Some(elems.head.text)
    }
  }

  /**
   * Get the selected made or down.
   * @return None if nothing is selected, Some(n) if n is selected
   * @throws TestFailedException if more than one is selected
   */
  def getNames(implicit patienceConfig: PatienceConfig, pos: Position) = {

    val x = s"""//button[starts-with( @id, 'P_' )]"""

    val elems = findElemsByXPath(x)

    elems.size mustBe 4
    elems.map(e => e.text)
  }

  def selectScorekeeper( name: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(toNameButton(name))
    this
  }

  def findPosButtons(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val x = s"""//button[starts-with( @id, 'SK_' )]"""

    val elems = findElemsByXPath(x)

    if (elems.isEmpty) Nil
    else {
      elems.size mustBe 2
      elems.map(e => scorekeeperButtons.get(e.attribute("id").get).get)
    }
  }

  def clickPos( sk: PlayerPosition )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(toScorekeeperButton(sk))
    new TableSelectScorekeeperPage(dupid,tableid,roundid,targetBoard,Some(sk))
  }

  def findSelectedPos: Option[PlayerPosition] = logMethod(s"getSelectedPos"){

    val x = s"""//button[starts-with( @id, 'SK_' ) and contains(concat(' ', @class, ' '), ' baseButtonSelected ')]"""

    val elems = findElemsByXPath(x)

    if (elems.isEmpty) None
    else {
      elems.size mustBe 1
      val id = elems.head.attribute("id").get
      scorekeeperButtons.get(id)
    }
  }

  def clickOK(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonOK)
    new TableSelectNamesPage(dupid,tableid,roundid,targetBoard,scorekeeper.get)
  }

  def clickCancel(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonCancel)
    new TablePage(dupid,tableid,EnterNames)
  }

  def isOKEnabled(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getButton(buttonOK).isEnabled
  }

  def verifyAndSelectScorekeeper(
                         north: String, south: String, east: String, west: String,
                         scorekeeper: PlayerPosition
                       )( implicit
                           webDriver: WebDriver
                       ) = {

    val skname = scorekeeper.player(north, south, east, west)

    getSelectedScorekeeper mustBe None
    selectScorekeeper(skname.trim)
    getSelectedScorekeeper mustBe Some(skname.trim)
    getNames must contain allOf (north.trim,south.trim,east.trim,west.trim)
    findPosButtons must contain allOf ( scorekeeper, scorekeeper.partner)
    val ss1 = clickPos(scorekeeper)
    eventually { ss1.findSelectedPos mustBe Some(scorekeeper) }
    ss1.clickOK.validate
  }

}
