package com.example.pages.duplicate

import com.example.pages.Page
import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.pages.PageBrowser._
import com.example.test.selenium.TestServer
import utils.logging.Logger
import com.example.test.util.HttpUtils
import com.example.data.BoardSet
import com.example.data.Movement
import java.net.URL
import com.example.pages.duplicate.ScoreboardPage.CompletedViewType
import com.example.pages.duplicate.ScoreboardPage.TableViewType
import com.example.pages.GenericPage
import com.example.pages.Page.AnyPage

object TablePage {

  val log = Logger[TablePage]

  def current(target: Target)(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (dupid,tableid) = findTableId
    new TablePage(dupid,tableid,target)
  }

  def urlFor( dupid: String, tableid: String ) = TestServer.getAppPageUrl( s"duplicate/${dupid}/table/${tableid}" )

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
    val prefix = TestServer.getAppPageUrl("duplicate/")
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

}
