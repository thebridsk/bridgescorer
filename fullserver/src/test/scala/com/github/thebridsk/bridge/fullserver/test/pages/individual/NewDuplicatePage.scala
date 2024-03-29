package com.github.thebridsk.bridge.fullserver.test.pages.individual

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
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.{ MovementsPage => TMovementsPage }
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.{ BoardSetsPage => TBoardSetsPage }
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.{ ScoreboardPage => TScoreboardPage }
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.{ DuplicateResultEditPage => TDuplicateResultEditPage }
import com.github.thebridsk.browserpages.GenericPage

object NewDuplicatePage {

  val log: Logger = Logger[NewDuplicatePage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): NewDuplicatePage = {
    currentUrl mustBe urlFor
    val gp = GenericPage.current
    val playI = gp.findButton(buttonPlayIndividual).containsClass("baseButtonSelected")
    new NewDuplicatePage(playI)
  }

  def goto(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): NewDuplicatePage = {
    go to urlFor
    new NewDuplicatePage(false)
  }

  def urlFor: String = TestServer.getAppPageUrl("individual/new")

  def movType(isIndividual: Boolean) = if (isIndividual) "_I_" else "_T_"

  def buttonName(
      boardset: String,
      movement: String,
      isIndividual: Boolean
  ): String = {
    isBoardsetAndMovementValid(boardset, movement, isIndividual)
    s"New${movType(isIndividual)}${movement}_${boardset}"
  }

  val result = "Result"

  def buttonNameResult(
      movement: String,
      isIndividual: Boolean
  ): String = {
    buttonName(result, movement, isIndividual)
  }

  def isBoardsetAndMovementValid(
      boardset: String,
      movement: String,
      isIndividual: Boolean
  )(implicit
      pos: Position
  ): Boolean = {
    var r = true
    if (!TBoardSetsPage.boardsets.contains(boardset) && boardset!="Result") {
      log.warning(s"Unknown boardset $boardset")
      r = false
    }
    if (isIndividual) {
      if (!TMovementsPage.movements.contains(movement)) {
        log.warning(s"Unknown movement $movement")
        r = false
      }
    } else {
      if (!IndividualMovementsPage.movementsIndividual.contains(movement)) {
        log.warning(s"Unknown movement $movement")
        r = false
      }
    }
    r
  }

  val buttonPlayIndividual = "PlayIndividual"
  val buttonPlayTeams = "PlayTeams"

  val buttonsPlay = buttonPlayIndividual :: buttonPlayTeams :: Nil
}

class NewDuplicatePage(
  playIndividual: Boolean
)(
  implicit
    webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[NewDuplicatePage] {
  import NewDuplicatePage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): NewDuplicatePage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        currentUrl mustBe urlFor

        val buttons = TBoardSetsPage.boardsets.flatMap { b =>
          s"ShowB_${b}" :: (
            if (playIndividual) {
              IndividualMovementsPage.movementsIndividual.flatMap { m =>
                if (m == "Individual2Tables21Boards" && (b == "ArmonkBoards" || b == "Result")) Nil
                else buttonName(b, m, true) :: Nil
              }
            } else {
              TMovementsPage.movements.flatMap { m =>
                if (m == "Howell3TableNoRelay" && b == "ArmonkBoards") Nil
                else buttonName(b, m, false) :: Nil
              }
            }
          )
        } ::: (
          if (playIndividual) {
            IndividualMovementsPage.movementsIndividual.map(m => s"ShowM${movType(true)}${m}")
          } else {
            TMovementsPage.movements.map(m => s"ShowM${movType(false)}${m}")
          }
        ) :::
           buttonsPlay

        findButtons(buttons: _*)
        this
      }
    }

  def clickPlayIndividual(implicit patienceConfig: PatienceConfig, pos: Position): NewDuplicatePage = {
    if (playIndividual) throw new Exception("Already in play individual")
    clickButton(buttonPlayIndividual)
    new NewDuplicatePage(true)
  }

  def clickPlayTeams(implicit patienceConfig: PatienceConfig, pos: Position): NewDuplicatePage = {
    if (!playIndividual) throw new Exception("Already in play teams")
    clickButton(buttonPlayTeams)
    new NewDuplicatePage(false)
  }

  def getNewButton(
      boardset: String,
      movement: String,
      isIndividual: Boolean
  )(
    implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Element = {
    findElemById(buttonName(boardset, movement, isIndividual))
  }

  def getNewResultsButton(
      movement: String,
      isIndividual: Boolean
  )(
    implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Element = {
    findElemById(buttonNameResult(movement, isIndividual))
  }

  /**
    *
    *
    * @param boardset
    * @param movement
    * @param isIndividual
    * @param patienceConfig
    * @param pos
    * @return The team scoreboard page or the individual scoreboard page depending
    *         on the value of the isIndividual argument.
    */
  def click(
      boardset: String,
      movement: String
  )(
    implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TScoreboardPage = {
    clickButton(buttonName(boardset, movement, false))
    new TScoreboardPage
  }

  /**
    *
    *
    * @param boardset
    * @param movement
    * @param patienceConfig
    * @param pos
    * @return The team scoreboard page or the individual scoreboard page depending
    *         on the value of the isIndividual argument.
    */
  def clickIndividual(
      boardset: String,
      movement: String
  )(
    implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ScoreboardPage = {
    clickButton(buttonName(boardset, movement, true))
    new ScoreboardPage("")
  }

  /**
    *
    *
    * @param boardset
    * @param movement
    * @param isIndividual using individual movement.  Not supported yet.  Default is false.
    * @param patienceConfig
    * @param pos
    * @return the page object for the expected browser page
    */
  def clickResultsOnly(
      movement: String,
      isIndividual: Boolean = false
  )(
    implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TDuplicateResultEditPage = {
    click("Result", movement)  // return is ignored
    if (isIndividual) new TDuplicateResultEditPage
    else new TDuplicateResultEditPage
  }

  def clickBoardSet(
      boardset: String
  )(implicit patienceConfig: PatienceConfig, pos: Position): TBoardSetsPage = {
    if (!TBoardSetsPage.boardsets.contains(boardset))
      log.warning(s"Unknown boardset $boardset")

    clickButton(s"ShowB_${boardset}")
    new TBoardSetsPage(Option(boardset))
  }

  def clickMovement(
      movement: String,
      isIndividual: Boolean
  )(implicit patienceConfig: PatienceConfig, pos: Position): TMovementsPage = {
    if (!TMovementsPage.movements.contains(movement))
      log.warning(s"Unknown movement $movement")

    clickButton(s"ShowM${movType(isIndividual)}${movement}")
    new TMovementsPage
  }
}
