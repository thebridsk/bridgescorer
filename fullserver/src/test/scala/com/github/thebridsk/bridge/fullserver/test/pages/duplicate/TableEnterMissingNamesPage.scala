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
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.ErrorMsgDiv
import com.github.thebridsk.browserpages.Element

object TableEnterMissingNamesPage {

  val log: Logger = Logger[TableEnterMissingNamesPage]()

  def current(targetBoard: Option[String])(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterMissingNamesPage = {
    val (dupid, tableid, roundid, targetboard) = findTableRoundId
    new TableEnterMissingNamesPage(dupid, tableid, roundid, targetBoard)
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

  private val patternInputName = """I_([a-zA-Z])""".r

  val buttonOK = "OK"
  val buttonReset = "Reset"
  val buttonCancel = "Cancel"
}

class TableEnterMissingNamesPage(
    dupid: String,
    tableid: String,
    roundid: String,
    targetBoard: Option[String]
)(implicit
    val webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[TableEnterMissingNamesPage]
    with ErrorMsgDiv[TableEnterMissingNamesPage] {
  import TableEnterMissingNamesPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterMissingNamesPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        currentUrl mustBe urlFor(dupid, tableid, roundid, targetBoard)

        findButtons(buttonOK, buttonReset, buttonCancel)

        getInputFieldNames.size mustBe 2
        this
      }
    }

  def getInputFieldNames(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): List[PlayerPosition] =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.getInputFieldNames") {
      val inputs = getAllInputs(Some("text"))
      log.fine(
        s"${pos.line} ${getClass.getSimpleName}.getInputFieldNames: found keys ${inputs.keySet}"
      )
      if (inputs.isEmpty)
        log.fine(
          s"${pos.line} ${getClass.getSimpleName}.getInputFieldNames: current URL is ${currentUrl}"
        )
      inputs.keys
        .flatMap(key =>
          key match {
            case patternInputName(p) => p :: Nil
            case _                   => Nil
          }
        )
        .map(p => PlayerPosition(p))
        .toList

    }

  /**
    * Enter a players name.  Of the locations, only three will be on the screen.  The scorekeeper's name
    * has already been entered on the previous screen.
    * @param loc the location on the screen.  The scorekeeper's location is not valid.
    */
  def enterPlayer(loc: PlayerPosition, name: String)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterMissingNamesPage = {
    val text = eventually {
      getTextInput(toInputName(loc))
    }
    text.value = name
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
  ): TableSelectScorekeeperPage = {
    clickButton(buttonOK)
    new TableSelectScorekeeperPage(dupid, tableid, roundid, targetBoard)
  }

  def clickReset(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterMissingNamesPage = {
    clickButton(buttonReset)
    this
  }

  def clickCancel(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ScoreboardPage = {
    clickButton(buttonCancel)
    new ScoreboardPage(Some(dupid), TableViewType(tableid, roundid))
  }

  def isOKEnabled(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    getButton(buttonOK).isEnabled
  }
}
