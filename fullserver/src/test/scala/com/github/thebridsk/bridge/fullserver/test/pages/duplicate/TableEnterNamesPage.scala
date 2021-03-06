package com.github.thebridsk.bridge.fullserver.test.pages.duplicate

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage.TableViewType
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.browserpages.Page.AnyPage
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.ErrorMsgDiv
import com.github.thebridsk.browserpages.Element

object TableEnterNamesPage {

  val log: Logger = Logger[TableEnterNamesPage]()

  def current(targetBoard: Option[String], scorekeeper: PlayerPosition)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterNamesPage = {
    val (dupid, tableid, roundid, targetboard) = findTableRoundId
    new TableEnterNamesPage(dupid, tableid, roundid, targetBoard, scorekeeper)
  }

  def urlFor(
      dupid: String,
      tableid: String,
      roundid: String,
      board: Option[String]
  ): String =
    TableEnterScorekeeperPage.urlFor(dupid, tableid, roundid, board)

  def goto(
      dupid: String,
      tableid: String,
      roundid: String,
      board: Option[String] = None
  )(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterScorekeeperPage =
    TableEnterScorekeeperPage.goto(dupid, tableid, roundid, board)

  /**
    * Get the table id
    * currentUrl needs to be one of the following:
    *   duplicate/dupid/table/tableid/round/roundid/teams
    * @return (dupid, tableid,roundid)
    */
  def findTableRoundId(implicit webDriver: WebDriver, pos: Position) =
    TableEnterScorekeeperPage.findTableRoundId

  private def toInputName(loc: PlayerPosition) = s"I_${loc.pos}"

  val buttonOK = "OK"
  val buttonReset = "Reset"
  val buttonCancel = "Cancel"
}

class TableEnterNamesPage(
    dupid: String,
    tableid: String,
    roundid: String,
    targetBoard: Option[String],
    scorekeeper: PlayerPosition
)(implicit
    val webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[TableEnterNamesPage]
    with ErrorMsgDiv[TableEnterNamesPage] {
  import TableEnterNamesPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterNamesPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        currentUrl mustBe urlFor(dupid, tableid, roundid, targetBoard)

        findButtons(buttonOK, buttonReset, buttonCancel)
        this
      }
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
      getTextInput(toInputName(loc))
    }
    text.value = name
    this
  }

  def enterPlayers(partner: String, left: String, right: String)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterNamesPage = {
    eventually {
      val p = getTextInput(toInputName(scorekeeper.partner))
      val l = getTextInput(toInputName(scorekeeper.left))
      val r = getTextInput(toInputName(scorekeeper.right))

      p.value = partner
      l.value = left
      r.value = right
    }
    this
  }

  /**
    * @param loc the location on the screen.  The scorekeeper's location is not valid.
    */
  def getPlayer(
      loc: PlayerPosition
  )(implicit patienceConfig: PatienceConfig, pos: Position): String = {
    eventually {
      getTextInput(toInputName(loc)).value
    }
  }

  /**
    * @param loc the location on the screen.  The scorekeeper's location is not valid.
    */
  def getPlayerSuggestions(
      loc: PlayerPosition
  )(implicit patienceConfig: PatienceConfig, pos: Position): List[Element] = {
    eventually {
      getCombobox(toInputName(loc)).suggestions
    }
  }

  /**
    * @param loc the location on the screen.  The scorekeeper's location is not valid.
    */
  def isPlayerSuggestionsVisible(
      loc: PlayerPosition
  )(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    eventually {
      getCombobox(toInputName(loc)).isSuggestionVisible
    }
  }

  def clickOK(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): AnyPage = {
    clickButton(buttonOK)
    targetBoard match {
      case Some(boardid) => HandPage.current
      case None =>
        new ScoreboardPage(Some(dupid), TableViewType(tableid, roundid))
    }
  }

  def clickOKAndValidate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): AnyPage = {
    clickButton(buttonOK)
    targetBoard match {
      case Some(boardid) => HandPage.current.validate
      case None =>
        new ScoreboardPage(
          Some(dupid),
          TableViewType(tableid, roundid)
        ).validate
    }
  }

  def clickReset(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterScorekeeperPage = {
    clickButton(buttonReset)
    new TableEnterScorekeeperPage(dupid, tableid, roundid, targetBoard, None)
  }

  def clickCancel(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TablePage = {
    clickButton(buttonCancel)
    new TablePage(dupid, tableid, TablePage.MissingNames)
//    new ScoreboardPage(Some(dupid), TableViewType(tableid,roundid))
  }

  def isOKEnabled(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    getButton(buttonOK).isEnabled
  }
}
