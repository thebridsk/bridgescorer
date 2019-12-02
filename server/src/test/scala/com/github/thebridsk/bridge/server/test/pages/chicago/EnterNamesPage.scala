package com.github.thebridsk.bridge.server.test.pages.chicago

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import java.net.URL
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.browserpages.Page.AnyPage

object EnterNamesPage {

  val log = Logger[EnterNamesPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (chiid,roundid) = findMatchRoundId
    new EnterNamesPage(chiid,roundid)
  }

  def currentWithId(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val (chiid,roundid) = findMatchRoundId
    new EnterNamesPage(chiid,roundid)
  }

  /**
   * @param chiid the chicago id
   * @param roundid the round ID, zero based.
   */
  def urlFor( chiid: String, roundid: Int ) = {
    TestServer.getAppPageUrl( s"chicago/${chiid}/rounds/${roundid}/names" )
  }

  /**
   * @param chiid the chicago id
   * @param roundid the round ID, zero based.
   */
  def demoUrlFor( chiid: String, roundid: Int ) = {
    TestServer.getAppDemoPageUrl( s"chicago/${chiid}/rounds/${roundid}/names" )
  }

  /**
   * @param chiid the chicago id
   * @param roundid the round ID, zero based.
   */
  def goto( chiid: String, roundid: Int )( implicit
              webDriver: WebDriver,
              patienceConfig: PatienceConfig,
              pos: Position
          ) = {
    go to urlFor(chiid,roundid)
    new EnterNamesPage(chiid,roundid)
  }

  private val patternUrl = """(C\d+)/rounds/(\d+)/names""".r

  /**
   * Get the table id
   * currentUrl needs to match the following:
   *   chicago/{chiid}/rounds/{roundid}/names
   * @return (chiid, roundid)
   *          chiid the chicago id
   *          roundid the round ID, zero based.
   */
  def findMatchRoundId(implicit webDriver: WebDriver, pos: Position): (String,Int) = {
    val prefix = TestServer.getAppPageUrl("chicago/")
    val prefix2 = TestServer.getAppDemoPageUrl("chicago/")
    val cur = currentUrl
    withClue(s"Unable to determine chicago id in EnterNamesPage: ${cur}") {
      val rest = if (cur.startsWith(prefix)) {
        cur.drop(prefix.length())
      } else if (cur.startsWith(prefix2)) {
        cur.drop(prefix2.length())
      } else {
        fail(s"URL did not start with $prefix2 or $prefix: $cur")
      }
      rest match {
        case patternUrl(chiid,rid) => (chiid,rid.toInt)
        case _ => fail(s"URL did not match pattern ${patternUrl}")
      }
    }
  }

  private def toInputName( loc: PlayerPosition ) = loc.name
  val sittingOutInputName = "Extra"
  val fastRotationCheckboxName = "Quintet"
  val simpleRotationRadioBoxName = "Simple"
  val fairRotationRadioBoxName = "Fair"

  private def toDealerButtonId( loc: PlayerPosition ) = s"Player${loc.pos}FirstDealer"

  val buttonOK = "Ok"
  val buttonReset = "ResetNames"
  val buttonToggleFive = "ToggleFive"     // shows "Five" or "Four" on button
  val buttonCancel = "Cancel"
}

class EnterNamesPage( val chiid: String,
                      val roundid: Int,
                    )( implicit
                         webDriver: WebDriver,
                         pageCreated: SourcePosition
                    ) extends Page[EnterNamesPage] {
  import EnterNamesPage._
  import com.github.thebridsk.bridge.data.bridge._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    roundid.toString() mustBe "0"      // only valid for the first round

    Some(currentUrl) must contain oneOf( urlFor(chiid,roundid), demoUrlFor(chiid,roundid) )

    val dealerbuttons = (North::South::East::West::Nil).map(p=>toDealerButtonId(p))

    val allButtons = buttonOK::buttonReset::buttonCancel::buttonToggleFive :: dealerbuttons

    findButtons( allButtons: _* )
    this
  }}

  /**
   * Enter a player's name.
   * @param loc the location on the screen.
   * @param name
   */
  def enterPlayer( loc: PlayerPosition, name: String, hitEscapeAfter: Boolean = false )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val text = eventually {
      getCombobox(toInputName(loc))
    }
    text.value = name
    if (hitEscapeAfter) text.esc
    this
  }

  /**
   * Enter sitting out player's name.
   * @param name
   */
  def enterSittingOutPlayer( name: String, hitEscapeAfter: Boolean = false )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val text = eventually {
      getCombobox(sittingOutInputName)
    }
    text.value = name
    if (hitEscapeAfter) text.esc
    this
  }

  /**
   * @param loc the location on the screen.  The scorekeeper's location is not valid.
   */
  def getPlayer( loc: PlayerPosition )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox(toInputName(loc)).value
    }
  }

  /**
   */
  def getSittingOutPlayer()(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox(sittingOutInputName).value
    }
  }

  /**
   * @param loc the location on the screen.  The scorekeeper's location is not valid.
   */
  def getPlayerSuggestions( loc: PlayerPosition )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox(toInputName(loc)).suggestions
    }
  }

  /**
   */
  def getSittingOutPlayerSuggestions()(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox(sittingOutInputName).suggestions
    }
  }

  /**
   * @param loc the location on the screen.  The scorekeeper's location is not valid.
   */
  def isPlayerSuggestionsVisible( loc: PlayerPosition )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox(toInputName(loc)).isSuggestionVisible
    }
  }

  /**
   */
  def isSittingOutPlayerSuggestionsVisible()(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox(sittingOutInputName).isSuggestionVisible
    }
  }

  def isDealer( loc: PlayerPosition )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getButton( toDealerButtonId(loc)).containsClass("baseButtonSelected")
  }

  def setDealer( loc: PlayerPosition )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton( toDealerButtonId(loc) )
    this
  }

  def clickOK(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonOK)
    new HandPage(chiid,roundid,0)
  }

  def clickReset(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonReset)
    this
  }

  def clickCancel(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonCancel)
    GenericPage.current
  }

  def isOKEnabled(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getButton(buttonOK).isEnabled
  }

  def clickFive(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonToggleFive)
    this
  }

  def isFive(implicit pos: Position) = {
    findButton(buttonToggleFive).text == "Four"    // the button shows target, not what is showing
  }

  def isSittingOutVisible(implicit pos: Position) = {
    val tr = findElemByXPath("//div[contains(concat(' ', @class, ' '), ' chiViewPlayersVeryFirstRound ')]/table/tbody/tr[1]")
    tr.attribute("class") match {
      case Some(cls) =>
        cls.split(" ").contains("baseAlwaysHide")
      case None =>
        true
    }
  }

  def isFastRotation(implicit pos: Position) = {
    findCheckbox(fastRotationCheckboxName).isSelected
  }

  def isSimpleRotation(implicit pos: Position) = {
    findRadioButton(simpleRotationRadioBoxName).isSelected
  }

  def isFairRotation(implicit pos: Position) = {
    findRadioButton(fairRotationRadioBoxName).isSelected
  }

  def clickFastRotation(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(fastRotationCheckboxName)
    this
  }

  def clickSimpleRotation(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(simpleRotationRadioBoxName)
    this
  }

  def clickFairRotation(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(fairRotationRadioBoxName)
    this
  }
}
