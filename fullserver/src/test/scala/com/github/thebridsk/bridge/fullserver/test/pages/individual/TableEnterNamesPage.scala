package com.github.thebridsk.bridge.fullserver.test.pages.individual

import com.github.thebridsk.browserpages.Page
import org.openqa.selenium.WebDriver
import com.github.thebridsk.source.SourcePosition
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.Eventually._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.PageBrowser._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.browserpages.Element

object TableEnterNamesPage {

  val log: Logger = Logger[TablePage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterNamesPage = {
    val (dupid, tableid, roundid, board) = findTableId
    new TableEnterNamesPage(dupid, tableid, roundid, board)
  }

  def urlFor(
      dupid: String,
      tableid: String,
      roundid: String,
      board: Int
  ): String =
    TestServer.getAppPageUrl(s"individual/match/${dupid}/table/${tableid}/round/${roundid}/boards/B${board}/teams")

  def goto(
      dupid: String,
      tableid: String,
      roundid: String,
      board: Int
  )(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterNamesPage = {
    go to urlFor(dupid, tableid, roundid, board)
    new TableEnterNamesPage(dupid, tableid, roundid, Some(board))
  }

  private val patternTable = """(M\d+)/table/(\d+)/round/(\d+)(?:/boards/B(\d+))?/teams""".r

  /**
    * Get the table id
    * currentUrl needs to be one of the following:
    *   duplicate/dupid/table/tableid
    * @return (dupid, tableid)
    */
  def findTableId(implicit
      webDriver: WebDriver,
      pos: Position
  ): (String, String, String, Option[Int]) = {
    val prefix = TestServer.getAppPageUrl("individual/match/")
    val cur = currentUrl
    withClue(s"Unable to determine duplicate id: ${cur}") {
      cur must startWith(prefix)
      cur.drop(prefix.length()) match {
        case patternTable(did, tid, rid, bid) =>
          if (bid == null || bid == "") {
            (did, tid, rid, None)
          } else {
            (did, tid, rid, Some(bid.toInt))
          }
        case _                      => fail("Could not determine table")
      }
    }
  }

  val buttonOK = "OK"
  val buttonCancel = "Cancel"
  val buttonReset = "Reset"

  val buttons =
    buttonOK ::
    buttonReset ::
    buttonCancel ::
    "SK_North" ::
    "SK_South" ::
    "SK_East" ::
    "SK_West" ::
    Nil

  def posToInput(pos: PlayerPosition) = s"${pos.toString()}_input"
  def posToName(pos: PlayerPosition) = s"${pos.toString()}_name"
  def posToSK(pos: PlayerPosition) = s"SK_${pos.toString()}"

}

class TableEnterNamesPage(
  dupid: String,
  tableid: String,
  roundid: String,
  board: Option[Int]
)(
    implicit
    webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[TableEnterNamesPage] {
  import TableEnterNamesPage._

  def validate(implicit patienceConfig: Eventually.PatienceConfig, pos: Position): TableEnterNamesPage = {
    findButtons(buttons:_*)
    this
  }

  def isName(
      loc: PlayerPosition
  )(
    implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = try {
    getElemById(posToName(loc))
    true
  } catch {
    case _: Exception =>
      false
  }

  def getName(
      loc: PlayerPosition
  )(
    implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): String = {
    getElemById(posToName(loc)).text
  }

  def isInput(
      loc: PlayerPosition
  )(
    implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = try {
    getElemById(posToInput(loc))
    true
  } catch {
    case _: Exception =>
      false
  }

  /**
    * Enter a players name.  Of the locations, only three will be on the screen.  The scorekeeper's name
    * has already been entered on the previous screen.
    * @param loc the location on the screen.  The scorekeeper's location is not valid.
    */
  def enterPlayer(loc: PlayerPosition, name: String)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterNamesPage = {
    val text = eventually {
      getTextInput(posToInput(loc))
    }
    text.value = name
    this
  }

  /**
    * @param loc the location on the screen.  The scorekeeper's location is not valid.
    */
  def getEnteredPlayer(
      loc: PlayerPosition
  )(implicit patienceConfig: PatienceConfig, pos: Position): String = {
    eventually {
      getTextInput(posToInput(loc)).value
    }
  }

  /**
    * @param loc the location on the screen.  The scorekeeper's location is not valid.
    */
  def getPlayerSuggestions(
      loc: PlayerPosition
  )(implicit patienceConfig: PatienceConfig, pos: Position): List[Element] = {
    eventually {
      getCombobox(posToInput(loc)).suggestions
    }
  }

  /**
    * @param loc the location on the screen.  The scorekeeper's location is not valid.
    */
  def isPlayerSuggestionsVisible(
      loc: PlayerPosition
  )(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    eventually {
      getCombobox(posToInput(loc)).isSuggestionVisible
    }
  }

  def setScorekeeper(
      loc: PlayerPosition
  )(
    implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterNamesPage = {
    click on id(posToSK(loc))
    this
  }

  def isScorekeeper(
      loc: PlayerPosition
  )(
    implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    val e = getElemById(posToSK(loc))
    e.containsClass("baseButtonSelected")
  }

  def clickOK(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Page.AnyPage = {
    clickButton(buttonOK)
    board match {
      case Some(bid) =>
        new HandPage()
      case None =>
        new ScoreboardPage(Some(dupid),ScoreboardPage.TableViewType(tableid,roundid))
    }
  }

  def clickReset(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterNamesPage = {
    clickButton(buttonReset)
    this
  }

  def clickCancel(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TablePage = {
    clickButton(buttonCancel)
    new TablePage(dupid, tableid, TablePage.EnterNames)
  }

  def isOKEnabled(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    getButton(buttonOK).isEnabled
  }


}
