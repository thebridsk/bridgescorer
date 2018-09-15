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

object NewDuplicatePage {

  val log = Logger[NewDuplicatePage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new NewDuplicatePage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor
    new NewDuplicatePage
  }

  def urlFor = TestServer.getAppPageUrl("duplicate/#new")

}

class NewDuplicatePage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[NewDuplicatePage] {
  import NewDuplicatePage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

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

  def getNewButton( boardset: String, movement: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (!BoardSetsPage.boardsets.contains(boardset)) log.warning(s"Unknown boardset $boardset")
    if (!MovementsPage.movements.contains(movement)) log.warning(s"Unknown movement $movement")
    findElemById(s"New_${movement}_${boardset}")
  }

  def click( boardset: String, movement: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (!BoardSetsPage.boardsets.contains(boardset)) log.warning(s"Unknown boardset $boardset")
    if (!MovementsPage.movements.contains(movement)) log.warning(s"Unknown movement $movement")
    clickButton(s"New_${movement}_${boardset}")
    new ScoreboardPage
  }

  def clickForResultsOnly( boardset: String, movement: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (!BoardSetsPage.boardsets.contains(boardset)) log.warning(s"Unknown boardset $boardset")
    if (!MovementsPage.movements.contains(movement)) log.warning(s"Unknown movement $movement")
    clickButton(s"New_${movement}_${boardset}")
    new DuplicateResultEditPage
  }

  def clickBoardSet( boardset: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (!BoardSetsPage.boardsets.contains(boardset)) log.warning(s"Unknown boardset $boardset")

    clickButton(s"ShowB_${boardset}")
    new BoardSetsPage(Option(boardset))
  }

  def clickMovement( movement: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (!MovementsPage.movements.contains(movement)) log.warning(s"Unknown movement $movement")

    clickButton(s"ShowM_${movement}")
    new MovementsPage
  }

  def isCreateResultsOnly(implicit patienceConfig: PatienceConfig, pos: Position) = {
    findCheckbox("resultsOnly").isSelected
  }

  def clickCreateResultsOnly(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually { findCheckbox("resultsOnly").click }
    this
  }
}
