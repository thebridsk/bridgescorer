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
import com.github.thebridsk.bridge.fullserver.test.pages.individual.ScoreboardPage.CompletedViewType
import com.github.thebridsk.bridge.fullserver.test.pages.individual.ScoreboardPage.TableViewType
import com.github.thebridsk.browserpages.Page.AnyPage
import com.github.thebridsk.bridge.fullserver.test.pages.BaseHandPage
import com.github.thebridsk.bridge.data.IndividualBoard

object TablePage {

  val log: Logger = Logger[TablePage]()

  def current(target: Target)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): TablePage = {
    val (dupid, tableid) = findTableId
    new TablePage(dupid, tableid, target)
  }

  def urlFor(dupid: String, tableid: String): String =
    TestServer.getAppPageUrl(s"individual/match/${dupid}/table/${tableid}")

  def goto(dupid: String, tableid: String, target: Target)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): TablePage = {
    go to urlFor(dupid, tableid)
    new TablePage(dupid, tableid, target)
  }

  private val patternTable = """(I\d+)/table/(\d+)""".r

  /**
    * Get the table id
    * currentUrl needs to be one of the following:
    *   duplicate/dupid/table/tableid
    * @return (dupid, tableid)
    */
  def findTableId(implicit
      webDriver: WebDriver,
      pos: Position
  ): (String, String) = {
    val prefix = TestServer.getAppPageUrl("individual/match/")
    val cur = currentUrl
    withClue(s"Unable to determine duplicate id from URL: ${cur}") {
      cur must startWith(prefix)
      cur.drop(prefix.length()) match {
        case patternTable(did, tid) => (did, tid)
        case _                      => fail("Could not determine table")
      }
    }
  }

  sealed trait Target

  object EnterNames extends Target
  object Hands extends Target
  object Boards extends Target

  val Results = Hands

}

class TablePage(
    val dupid: String,
    val tableid: String,
    val target: TablePage.Target
)(
    implicit
    webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[TablePage] {
  import TablePage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TablePage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        currentUrl mustBe urlFor(dupid, tableid)

        findButtons("Game", "InputStyle")
        this
      }
    }

  def validate(
      rounds: List[Int]
  )(implicit patienceConfig: PatienceConfig, pos: Position): TablePage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        currentUrl mustBe urlFor(dupid, tableid)

        findButtons("Game" :: "InputStyle" :: rounds.map { r =>
          s"Round_${r}"
        }: _*)
        this
      }
    }

  def setTarget(ntarget: Target) = new TablePage(dupid, tableid, ntarget)

  def clickRound(
      round: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): AnyPage = {
    clickButton(s"Round_${round}")
    target match {
      case EnterNames =>
        new TableEnterNamesPage(dupid, tableid, round.toString(), None)
      case Hands | Boards =>
        new ScoreboardPage(
          dupid,
          TableViewType(tableid, round.toString())
        )
    }
  }

  def clickBoard(round: Int, board: Int)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): AnyPage = {
    clickButton(s"Board_B${board}")
    target match {
      case EnterNames =>
        new TableEnterNamesPage(dupid, tableid, round.toString(), Some(board))
      case Hands =>
        new HandPage(dupid, TableViewType(tableid, round.toString()), IndividualBoard.id(board).id, "")
      case Boards =>
        new BoardPage(dupid, TableViewType(tableid, round.toString()), IndividualBoard.id(board).id)
    }
  }

  def clickCompletedScoreboard(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ScoreboardPage = {
    clickButton("Game")
    new ScoreboardPage(dupid, CompletedViewType)
  }

  def clickInputStyle(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): TablePage = {
    clickButton("InputStyle")
    this
  }

  def getInputStyle(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): Option[String] = {
    BaseHandPage.getInputStyle
  }

  def setInputStyle(
      want: String
  )(implicit
      webDriver: WebDriver,
      pos: Position
  ): Option[String] = {
    BaseHandPage.setInputStyle(want)
  }

}
