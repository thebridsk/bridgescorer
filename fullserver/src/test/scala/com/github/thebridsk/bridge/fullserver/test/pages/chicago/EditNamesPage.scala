package com.github.thebridsk.bridge.fullserver.test.pages.chicago

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.ErrorMsgDiv

object EditNamesPage {

  val log = Logger[EditNamesPage]()

  def current( matchType: ChicagoMatchType )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val chiid = findMatchId
    new EditNamesPage(chiid, matchType)
  }

  /**
   * @param chiid the chicago id
   * @param roundid the round ID, zero based.
   */
  def urlFor( chiid: String ) = {
    TestServer.getAppPageUrl( s"chicago/${chiid}/names" )
  }

  /**
   * @param chiid the chicago id
   * @param roundid the round ID, zero based.
   */
  def demoUrlFor( chiid: String ) = {
    TestServer.getAppDemoPageUrl( s"chicago/${chiid}/names" )
  }

  /**
   * @param chiid the chicago id
   */
  def goto( chiid: String, matchType: ChicagoMatchType )( implicit
              webDriver: WebDriver,
              patienceConfig: PatienceConfig,
              pos: Position
          ) = {
    go to urlFor(chiid)
    new EditNamesPage(chiid, matchType)
  }

  private val patternUrl = """(C\d+)/names""".r

  /**
   * Get the table id
   * currentUrl needs to match the following:
   *   chicago/{chiid}/rounds/{roundid}/names
   * @return chiid the chicago id
   */
  def findMatchId(implicit webDriver: WebDriver, pos: Position): String = {
    val prefix = TestServer.getAppPageUrl("chicago/")
    val prefix2 = TestServer.getAppDemoPageUrl("chicago/")
    val cur = currentUrl
    withClue(s"Unable to determine chicago id in EditNamesPage: ${cur}") {
      val rest = if (cur.startsWith(prefix)) {
        cur.drop(prefix.length())
      } else if (cur.startsWith(prefix2)) {
        cur.drop(prefix2.length())
      } else {
        fail(s"URL did not start with $prefix2 or $prefix: $cur")
      }
      rest match {
        case patternUrl(chiid) => chiid
        case _ => fail(s"URL did not match pattern ${patternUrl}")
      }
    }
  }

  val buttonOK = "OK"
  val buttonReset = "Reset"
  val buttonCancel = "Cancel"

  def toInputName( row: Int ) = s"I_$row"
}

class EditNamesPage(
    val chiid: String,
    val matchType: ChicagoMatchType
)( implicit
    val webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[EditNamesPage] with ErrorMsgDiv[EditNamesPage] {
  import EditNamesPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    Some(currentUrl) must (contain.oneOf( urlFor(chiid), demoUrlFor(chiid) ) )

    val allButtons = buttonOK::buttonReset::buttonCancel::Nil

    findButtons( allButtons: _* )
    this
  }}

  /**
   * Enter a player's new name.
   * @param row the location on the screen, 0 based.
   * @param name
   */
  def enterPlayer( row: Int, name: String, hitEscapeAfter: Boolean = false )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val text = eventually {
      getCombobox(toInputName(row))
    }
    text.value = name
    if (hitEscapeAfter) text.esc
    this
  }

  /**
   * Get the player's new name value
   * @param row the location on the screen, 0 based.
   */
  def getPlayer( row: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox(toInputName(row)).value
    }
  }

  /**
   * Get the suggestions for names
   * @param row the location on the screen, 0 based.
   */
  def getPlayerSuggestions( row: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox(toInputName(row)).suggestions
    }
  }

  def getCurrentPlayerNames(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' chiDivEditNamesPage ')]/table/tbody/tr/td[1]""").map(e => e.text)
  }

  def getNewPlayerNames(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' chiDivEditNamesPage ')]/table/tbody/tr/td[2]/div/div/input""").map(e => e.attribute("value").getOrElse(""))
  }

  /**
   * @param row the location on the screen, 0 based.
   */
  def getPlayerCombobox( row: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox(toInputName(row))
    }
  }

  /**
   * @param row the location on the screen, 0 based.
   */
  def isPlayerSuggestionsVisible( row: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox(toInputName(row)).isSuggestionVisible
    }
  }

  def clickOK(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonOK)
    new SummaryPage(chiid, matchType)
  }

  def clickReset(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonReset)
    this
  }

  def clickCancel(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonCancel)
    new SummaryPage(chiid, matchType)
  }

  def isOKEnabled(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getButton(buttonOK).isEnabled
  }
}
