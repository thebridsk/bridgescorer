package com.github.thebridsk.bridge.fullserver.test.pages.chicago

import com.github.thebridsk.browserpages.Page
import org.openqa.selenium.WebDriver
import com.github.thebridsk.source.SourcePosition
import com.github.thebridsk.utilities.logging.Logger
import org.scalatest.concurrent.Eventually._
import org.scalactic.source.Position
import com.github.thebridsk.browserpages.PageBrowser._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.browserpages.GenericPage
import org.scalatest.Assertion

object FourSelectPartnersPage {

  val log: Logger = Logger[FourSelectPartnersPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): FourSelectPartnersPage = {
    val (chiid, roundid) = EnterNamesPage.findMatchRoundId
    new FourSelectPartnersPage(chiid, roundid)
  }

  def urlFor(chiid: String, roundid: Int): String =
    EnterNamesPage.urlFor(chiid, roundid)
  def demoUrlFor(chiid: String, roundid: Int): String =
    EnterNamesPage.demoUrlFor(chiid, roundid)

  /**
    * @param chiid the chicago id
    * @param roundid the round ID, zero based.
    */
  def goto(chiid: String, roundid: Int)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): FourSelectPartnersPage = {
    go to urlFor(chiid, roundid)
    new FourSelectPartnersPage(chiid, roundid)
  }

  private def toDealerButtonId(loc: PlayerPosition) =
    s"Player${loc.pos}FirstDealer"

  val buttonOK = "Ok"
  val buttonReset = "Reset"
  val buttonResetNames = "ResetNames"
  val buttonCancel = "Cancel"
  val buttonChangeScorekeeper = "ChangeScoreKeeper"

  /**
    * @param loc the location of the player.
    *             East is the player to the left of dealer
    *             South is the partner of dealer
    *             West is the player to the right of dealer
    * @param i the player, values are: 1, 2, 3  Not all options are available.
    * @return the id
    */
  private def toPlayerButtonId(loc: PlayerPosition, i: Int) = s"${loc.name}$i"
}

/**
  * @param chiid the chicago match id
  * @param round the round, 0 based
  * @param webDriver
  * @param pageCreated
  */
class FourSelectPartnersPage(
    chiid: String,
    roundid: Int
)(implicit
    webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[FourSelectPartnersPage] {
  import FourSelectPartnersPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FourSelectPartnersPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        roundid.toString() must not be "0" // only valid for the first round

        Some(currentUrl) must (contain
          .oneOf(urlFor(chiid, roundid), demoUrlFor(chiid, roundid)))

        val dealerbuttons =
          (North :: South :: East :: West :: Nil).map(p => toDealerButtonId(p))

        val allButtons =
          buttonOK :: buttonCancel :: buttonChangeScorekeeper :: dealerbuttons

        findButtons(allButtons: _*)
        this
      }
    }

  def isDealer(
      loc: PlayerPosition
  )(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    getButton(toDealerButtonId(loc)).containsClass("baseButtonSelected")
  }

  def setDealer(loc: PlayerPosition)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FourSelectPartnersPage = {
    clickButton(toDealerButtonId(loc))
    this
  }

  def clickChangeScorekeeper(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): GenericPage = {
    clickButton(buttonChangeScorekeeper)
    new GenericPage
  }

  def clickOK(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): HandPage = {
    clickButton(buttonOK)
    new HandPage(chiid, roundid, 0, ChicagoMatchTypeFour)
  }

  def clickResetNames(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FourSelectPartnersPage = {
    clickButton(buttonResetNames)
    this
  }

  def clickReset(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FourSelectPartnersPage = {
    clickButton(buttonReset)
    this
  }

  def clickCancel(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SummaryPage = {
    clickButton(buttonCancel)
    SummaryPage.current(ChicagoMatchTypeFour)
  }

  def isOKEnabled(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    getButton(buttonOK).isEnabled
  }

  def isResetEnabled(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    getButton(buttonReset).isEnabled
  }

  def isResetNamesEnabled(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    getButton(buttonResetNames).isEnabled
  }

  /**
    * Click the player at the position, to set that player there.
    *
    * @param loc the location of the player.
    *             East is the player to the left of dealer
    *             South is the partner of dealer
    *             West is the player to the right of dealer
    * @param i the player, values are: 1, 2, 3  Not all options are available.
    * @param patienceConfig
    * @param pos
    * @return
    */
  def clickPlayer(loc: PlayerPosition, i: Int)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FourSelectPartnersPage = {
    clickButton(toPlayerButtonId(loc, i))
    this
  }

  def getPlayerName(loc: PlayerPosition, i: Int)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): String = {
    val but = findButton(toPlayerButtonId(loc, i))
    but.text
  }

  def checkPlayerNames(loc: PlayerPosition, players: String*)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Assertion = {
    val buttonPlayers = players.zipWithIndex.map {
      case (p, i) =>
        findButton(toPlayerButtonId(loc, i + 1)).text
    }

    buttonPlayers must contain theSameElementsAs (players)
  }

  /**
    * Click the player at the location, to set that player there.
    *
    * @param loc the location of the player.
    *             East is the player to the left of dealer
    *             South is the partner of dealer
    *             West is the player to the right of dealer
    * @param name the name of the player to be selected at the location
    * @param patienceConfig
    * @param pos
    * @return
    */
  def clickPlayer(loc: PlayerPosition, name: String)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FourSelectPartnersPage = {
    val but = findElem(
      xpath(s"""//button[contains(@id, '${loc.name}') and text()='$name']""")
    )
    but.click
    this
  }

  def checkPlayer(loc: PlayerPosition, name: String, selected: Boolean)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FourSelectPartnersPage = {
    withClue(s"Checking if $loc, $name, selected $selected") {
      val but = findElem(
        xpath(s"""//button[contains(@id, '${loc.name}') and text()='$name']""")
      )
      but.attribute("class") match {
        case Some(cls) =>
          s" ${cls} " must include(" baseButtonSelected ")
        case None =>
          if (selected) fail("baseButtonSelected class not found")
      }
    }
    this
  }

  def checkPlayer(loc: PlayerPosition, i: Int, selected: Boolean)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FourSelectPartnersPage = {
    withClue(s"Checking if $loc, $i, selected $selected") {
      val but =
        findElem(xpath(s"""//button[@id='${toPlayerButtonId(loc, i)}']"""))
      but.attribute("class") match {
        case Some(cls) =>
          s" ${cls} " must include(" baseButtonSelected ")
        case None =>
          if (selected) fail("baseButtonSelected class not found")
      }
    }
    this
  }

}
