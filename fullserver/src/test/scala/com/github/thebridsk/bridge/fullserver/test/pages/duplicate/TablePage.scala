package com.github.thebridsk.bridge.fullserver.test.pages.duplicate

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import java.net.URL
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage.CompletedViewType
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage.TableViewType
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.browserpages.Page.AnyPage
import com.github.thebridsk.bridge.fullserver.test.pages.BaseHandPage

object TablePage {

  val log = Logger[TablePage]

  def current(target: Target)(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (dupid,tableid) = findTableId
    new TablePage(dupid,tableid,target)
  }

  def urlFor( dupid: String, tableid: String ) = TestServer.getAppPageUrl( s"duplicate/match/${dupid}/table/${tableid}" )

  def goto( dupid: String, tableid: String, target: Target)(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor(dupid,tableid)
    new TablePage(dupid,tableid,target)
  }

  private val patternTable = """(M\d+)/table/(\d+)""".r

  /**
   * Get the table id
   * currentUrl needs to be one of the following:
   *   duplicate/dupid/table/tableid
   * @return (dupid, tableid)
   */
  def findTableId(implicit webDriver: WebDriver, pos: Position): (String,String) = {
    val prefix = TestServer.getAppPageUrl("duplicate/match/")
    val cur = currentUrl
    withClue(s"Unable to determine duplicate id: ${cur}") {
      cur must startWith (prefix)
      cur.drop( prefix.length() ) match {
        case patternTable(did,tid) => (did,tid)
        case _ => fail("Could not determine table")
      }
    }
  }

  sealed trait Target

  object MissingNames extends Target
  object EnterNames extends Target
  object SelectNames extends Target
  object EnterOrSelectNames extends Target
  object Hands extends Target
  object Boards extends Target
  val Results = Hands

}

class TablePage( dupid: String,
                 tableid: String,
                 target: TablePage.Target
               )( implicit
                   webDriver: WebDriver,
                   pageCreated: SourcePosition
               ) extends Page[TablePage] {
  import TablePage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor(dupid,tableid)

    findButtons( "Game", "InputStyle" )
    this
  }}

  def validate( rounds: List[Int])(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor(dupid,tableid)

    findButtons( "Game"::"InputStyle"::rounds.map{ r => s"Round_${r}"}: _* )
    this
  }}

  def setTarget( ntarget: Target ) = new TablePage(dupid,tableid,ntarget)

  def clickRound( round: Int )(implicit patienceConfig: PatienceConfig, pos: Position): AnyPage = {
    clickButton(s"Round_${round}")
    target match {
      case MissingNames =>
        new TableEnterMissingNamesPage( dupid, tableid, round.toString(), None )
      case EnterNames =>
        new TableEnterScorekeeperPage( dupid, tableid, round.toString(), None )
      case SelectNames =>
        new TableSelectScorekeeperPage( dupid, tableid, round.toString(), None )
      case Hands | Boards =>
        new ScoreboardPage( Some(dupid), TableViewType(tableid,round.toString()) )
      case EnterOrSelectNames =>
        new TableEnterOrSelectNamesPage( dupid, tableid, round.toString(), None )
    }
  }

  def clickBoard( round: Int, board: Int )(implicit patienceConfig: PatienceConfig, pos: Position): AnyPage = {
    clickButton(s"Board_B${board}")
    target match {
      case MissingNames =>
        new TableEnterMissingNamesPage( dupid, tableid, round.toString(), Some(board.toString()) )
      case EnterNames =>
        new TableEnterScorekeeperPage( dupid, tableid, round.toString(), Some(board.toString()) )
      case SelectNames =>
        new TableSelectScorekeeperPage( dupid, tableid, round.toString(), Some(board.toString()) )
      case EnterOrSelectNames =>
        new TableEnterOrSelectNamesPage( dupid, tableid, round.toString(), Some(board.toString()) )
      case Hands =>
        new HandPage
      case Boards =>
        new BoardPage
    }
  }

  def clickCompletedScoreboard(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Game")
    new ScoreboardPage( Some(dupid), CompletedViewType )
  }

  def clickInputStyle(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("InputStyle")
    this
  }

  def getInputStyle(
      implicit webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): Option[String] = {
    BaseHandPage.getInputStyle
  }

  def setInputStyle(
      want: String
  )(
      implicit
      webDriver: WebDriver,
      pos: Position
  ): Option[String] = {
    BaseHandPage.setInputStyle(want)
  }

}
