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
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.EnterNames
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.ErrorMsgDiv

object TableSelectScorekeeperPage {

  val log: Logger = Logger[TableSelectScorekeeperPage]()

  def current(scorekeeper: Option[PlayerPosition] = None)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableEnterScorekeeperPage = {
    val (dupid, tableid, roundid, targetboard) = findTableRoundId
    new TableEnterScorekeeperPage(
      dupid,
      tableid,
      roundid,
      targetboard,
      scorekeeper
    )
  }

  def urlFor(
      dupid: String,
      tableid: String,
      roundid: String,
      board: Option[String]
  ): String = {
    val b = board.map(bb => s"boards/B${bb}/").getOrElse("")
    TestServer.getAppPageUrl(
      s"duplicate/match/${dupid}/table/${tableid}/round/${roundid}/${b}teams"
    )
  }

  def goto(
      dupid: String,
      tableid: String,
      roundid: String,
      board: Option[String] = None,
      scorekeeper: Option[PlayerPosition] = None
  )(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableSelectScorekeeperPage = {
    go to urlFor(dupid, tableid, roundid, board)
    new TableSelectScorekeeperPage(dupid, tableid, roundid, board, scorekeeper)
  }

  /**
    * Get the table id
    * currentUrl needs to be one of the following:
    *   duplicate/dupid/table/tableid/round/roundid/teams
    * @return (dupid, tableid,roundid)
    */
  def findTableRoundId(implicit
      webDriver: WebDriver,
      pos: Position
  ): (String, String, String, Option[String]) =
    TableEnterScorekeeperPage.findTableRoundId

  private def toNameButton(name: String) = s"P_${name}"

  private def toScorekeeperButton(loc: PlayerPosition) = s"SK_${loc.pos}"

  private val scorekeeperButtons = (North :: South :: East :: West :: Nil)
    .map(p => (toScorekeeperButton(p), p))
    .toMap

  val buttonOK = "OK"
  val buttonReset = "Reset"
  val buttonCancel = "Cancel"

}

class TableSelectScorekeeperPage(
    dupid: String,
    tableid: String,
    roundid: String,
    targetBoard: Option[String],
    scorekeeper: Option[PlayerPosition] = None
)(implicit
    val webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[TableSelectScorekeeperPage]
    with ErrorMsgDiv[TableSelectScorekeeperPage] {
  import TableSelectScorekeeperPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableSelectScorekeeperPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        currentUrl mustBe urlFor(dupid, tableid, roundid, targetBoard)

        findButtons(buttonOK, buttonCancel, buttonReset)
        this
      }
    }

  /**
    * Get the selected made or down.
    * @return None if nothing is selected, Some(n) if n is selected
    * @throws TestFailedException if more than one is selected
    */
  def getSelectedScorekeeper(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Option[String] = {

    val x =
      s"""//button[starts-with( @id, 'P_' ) and contains(concat(' ', @class, ' '), ' baseButtonSelected ')]"""

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
  def getNames(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): List[String] = {

    val x = s"""//button[starts-with( @id, 'P_' )]"""

    val elems = findElemsByXPath(x)

    elems.size mustBe 4
    elems.map(e => e.text)
  }

  def selectScorekeeper(name: String)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableSelectScorekeeperPage = {
    clickButton(toNameButton(name))
    this
  }

  def findPosButtons(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): List[PlayerPosition] = {
    val x = s"""//button[starts-with( @id, 'SK_' )]"""

    val elems = findElemsByXPath(x)

    if (elems.isEmpty) Nil
    else {
      elems.size mustBe 4
      elems.map(e => scorekeeperButtons.get(e.attribute("id").get).get)
    }
  }

  def clickPos(sk: PlayerPosition)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableSelectScorekeeperPage = {
    clickButton(toScorekeeperButton(sk))
    new TableSelectScorekeeperPage(
      dupid,
      tableid,
      roundid,
      targetBoard,
      Some(sk)
    )
  }

  def findSelectedPos: Option[PlayerPosition] =
    logMethod(s"getSelectedPos") {

      val x =
        s"""//button[starts-with( @id, 'SK_' ) and contains(concat(' ', @class, ' '), ' baseButtonSelected ')]"""

      val elems = findElemsByXPath(x)

      if (elems.isEmpty) None
      else {
        elems.size mustBe 1
        val id = elems.head.attribute("id").get
        scorekeeperButtons.get(id)
      }
    }

  def clickOK(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TableSelectNamesPage = {
    clickButton(buttonOK)
    new TableSelectNamesPage(
      dupid,
      tableid,
      roundid,
      targetBoard,
      scorekeeper.get
    )
  }

  def clickCancel(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TablePage = {
    clickButton(buttonCancel)
    new TablePage(dupid, tableid, EnterNames)
  }

  def isOKEnabled(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    getButton(buttonOK).isEnabled
  }

  def checkScorekeeperPosEnabled(
      enabled: List[PlayerPosition],
      disabled: List[PlayerPosition]
  ): Unit = {
    getButtons(enabled.map(toScorekeeperButton(_)):_*).foreach { v =>
      withClue(s"checkScoreKeeperPosEnabled: button ${v._1} is disable, should be enabled") {
        v._2.isEnabled mustBe true
      }
    }
    getButtons(disabled.map(toScorekeeperButton(_)):_*).foreach { v =>
      withClue(s"checkScoreKeeperPosEnabled: button ${v._1} is enabled, should be disabled") {
        v._2.isEnabled mustBe false
      }
    }
  }

  /**
    * @param north
    * @param south
    * @param east
    * @param west
    * @param scorekeeper
    * @param screenShotDir tuple2, first is directory, second is filename
    */
  def verifyAndSelectScorekeeper(
      north: String,
      south: String,
      east: String,
      west: String,
      scorekeeper: PlayerPosition,
      screenShotDir: Option[(String, String)] = None,
      checkErrMsg: Boolean = false
  )(implicit
      webDriver: WebDriver
  ): TableSelectNamesPage = {

    val skname = scorekeeper.player(north, south, east, west)

    getSelectedScorekeeper mustBe None
    if (checkErrMsg) checkErrorMsg("Please select scorekeeper")
    selectScorekeeper(skname.trim)
    getSelectedScorekeeper mustBe Some(skname.trim)
    getNames must (contain.allOf(north.trim, south.trim, east.trim, west.trim))
    findPosButtons must (contain.allOf(North, South, East, West))
    checkScorekeeperPosEnabled(
      scorekeeper::scorekeeper.partner::Nil,
      scorekeeper.left::scorekeeper.right::Nil
    )
    if (checkErrMsg) checkErrorMsg("Please select scorekeeper's position")
    val ss1 = clickPos(scorekeeper)
    eventually { ss1.findSelectedPos mustBe Some(scorekeeper) }
    if (checkErrMsg) checkErrorMsg("")
    screenShotDir.foreach { e =>
      ss1.takeScreenshot(e._1, e._2)
    }
    ss1.clickOK.validate
  }

}
