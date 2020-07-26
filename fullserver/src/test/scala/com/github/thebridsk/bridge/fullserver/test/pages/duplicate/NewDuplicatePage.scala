package com.github.thebridsk.bridge.fullserver.test.pages.duplicate

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Element

object NewDuplicatePage {

  val log: Logger = Logger[NewDuplicatePage]()

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): NewDuplicatePage = {
    new NewDuplicatePage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): NewDuplicatePage = {
    go to urlFor
    new NewDuplicatePage
  }

  def urlFor: String = TestServer.getAppPageUrl("duplicate/new")

}

class NewDuplicatePage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[NewDuplicatePage] {
  import NewDuplicatePage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position): NewDuplicatePage = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor

    val buttons = MovementsPage.movements.flatMap { m =>
                    s"ShowM_${m}"::
                    BoardSetsPage.boardsets.flatMap { b =>
                      if (m == "Howell3TableNoRelay" && b == "ArmonkBoards") Nil
                      else s"New_${m}_${b}"::Nil
                    }
                  } ::: BoardSetsPage.boardsets.map(b => s"ShowB_${b}")

    findButtons( buttons: _* )
    this
  }}

  def getNewButton( boardset: String, movement: String )(implicit patienceConfig: PatienceConfig, pos: Position): Element = {
    if (!BoardSetsPage.boardsets.contains(boardset)) log.warning(s"Unknown boardset $boardset")
    if (!MovementsPage.movements.contains(movement)) log.warning(s"Unknown movement $movement")
    findElemById(s"New_${movement}_${boardset}")
  }

  def click( boardset: String, movement: String )(implicit patienceConfig: PatienceConfig, pos: Position): ScoreboardPage = {
    if (!BoardSetsPage.boardsets.contains(boardset)) log.warning(s"Unknown boardset $boardset")
    if (!MovementsPage.movements.contains(movement)) log.warning(s"Unknown movement $movement")
    clickButton(s"New_${movement}_${boardset}")
    new ScoreboardPage
  }

  def clickForResultsOnly( boardset: String, movement: String )(implicit patienceConfig: PatienceConfig, pos: Position): DuplicateResultEditPage = {
    if (!BoardSetsPage.boardsets.contains(boardset)) log.warning(s"Unknown boardset $boardset")
    if (!MovementsPage.movements.contains(movement)) log.warning(s"Unknown movement $movement")
    clickButton(s"New_${movement}_${boardset}")
    new DuplicateResultEditPage
  }

  def clickBoardSet( boardset: String )(implicit patienceConfig: PatienceConfig, pos: Position): BoardSetsPage = {
    if (!BoardSetsPage.boardsets.contains(boardset)) log.warning(s"Unknown boardset $boardset")

    clickButton(s"ShowB_${boardset}")
    new BoardSetsPage(Option(boardset))
  }

  def clickMovement( movement: String )(implicit patienceConfig: PatienceConfig, pos: Position): MovementsPage = {
    if (!MovementsPage.movements.contains(movement)) log.warning(s"Unknown movement $movement")

    clickButton(s"ShowM_${movement}")
    new MovementsPage
  }

  def isCreateResultsOnly(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    isCheckboxSelected("resultsOnly")
  }

  def clickCreateResultsOnly(implicit patienceConfig: PatienceConfig, pos: Position): NewDuplicatePage = {
    eventually {
      val e = findCheckbox("resultsOnly")
      e.checkClickable
      e.click
    }
    this
  }
}
