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
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.EnterNames
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.ErrorMsgDiv

object TableEnterScorekeeperPage {

  val log = Logger[TableEnterScorekeeperPage]

  def current(scorekeeper: Option[PlayerPosition] = None)(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (dupid,tableid,roundid,targetboard) = findTableRoundId
    new TableEnterScorekeeperPage(dupid,tableid,roundid,targetboard,scorekeeper)
  }

  def urlFor( dupid: String, tableid: String, roundid: String, board: Option[String] ) = {
    val b = board.map(bb => s"boards/B${bb}/").getOrElse("")
    TestServer.getAppPageUrl( s"duplicate/match/${dupid}/table/${tableid}/round/${roundid}/${b}teams" )
  }

  def goto( dupid: String,
            tableid: String,
            roundid: String,
            board: Option[String] = None,
            scorekeeper: Option[PlayerPosition] = None
          )( implicit
              webDriver: WebDriver,
              patienceConfig: PatienceConfig,
              pos: Position
          ) = {
    go to urlFor(dupid,tableid,roundid, board)
    new TableEnterScorekeeperPage(dupid,tableid,roundid,board,scorekeeper)
  }

  private val patternTable = """(M\d+)/table/(\d+)/round/(\d+)/(?:boards/(B\d+)/)?teams""".r

  /**
   * Get the table id
   * currentUrl needs to be one of the following:
   *   duplicate/dupid/table/tableid/round/roundid/teams
   * @return (dupid, tableid,roundid)
   */
  def findTableRoundId(implicit webDriver: WebDriver, pos: Position): (String,String,String,Option[String]) = {
    val prefix = TestServer.getAppPageUrl("duplicate/match/")
    val cur = currentUrl
    withClue(s"Unable to determine duplicate id: ${cur}") {
      cur must startWith (prefix)
      cur.drop( prefix.length() ) match {
        case patternTable(did,tid,bid,rid) => (did,tid,rid,Option(bid))
        case _ => fail("Could not determine table")
      }
    }
  }

  private def toScorekeeperButton( loc: PlayerPosition ) = s"SK_${loc.pos}"

  private val scorekeeperButtons = (North::South::East::West::Nil).map(p=>(toScorekeeperButton(p),p)).toMap

  val buttonOK = "OK"
  val buttonReset = "Reset"
  val buttonCancel = "Cancel"

}

class TableEnterScorekeeperPage( dupid: String,
                                 tableid: String,
                                 roundid: String,
                                 targetBoard: Option[String],
                                 scorekeeper: Option[PlayerPosition] = None
                               )( implicit
                                   val webDriver: WebDriver,
                                   pageCreated: SourcePosition
                               ) extends Page[TableEnterScorekeeperPage] with ErrorMsgDiv[TableEnterScorekeeperPage] {
  import TableEnterScorekeeperPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor(dupid,tableid,roundid,targetBoard)

    findButtons( buttonOK::buttonReset::buttonCancel::scorekeeperButtons.keys.toList : _* )
    this
  }}

  def enterScorekeeper( name: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val text = eventually {
      getTextInput("Scorekeeper")
    }
    text.value = name
    this
  }

  def getScorekeeper(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getTextInput("Scorekeeper").value
    }
  }

  def getScorekeeperSuggestions(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox("Scorekeeper").suggestions
    }
  }

  def isScorekeeperSuggestionsVisible(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      getCombobox("Scorekeeper").isSuggestionVisible
    }
  }

  def clickPos( sk: PlayerPosition )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(toScorekeeperButton(sk))
    new TableEnterScorekeeperPage(dupid,tableid,roundid,targetBoard,Some(sk))
  }

  def findSelectedPos: Option[PlayerPosition] = logMethod(s"getSelectedPos"){

    val x = s"""//button[starts-with( @id, 'SK_' ) and contains(concat(' ', @class, ' '), ' baseButtonSelected ')]"""

    val elems = findElemsByXPath(x)

    if (elems.isEmpty) None
    else {
      elems.size mustBe 1
      val id = elems.head.attribute("id").get
      scorekeeperButtons.get(id)
    }
  }

  def clickOK(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonOK)
    new TableEnterNamesPage(dupid,tableid,roundid,targetBoard,scorekeeper.get)
  }

  def clickCancel(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonCancel)
    new TablePage(dupid,tableid,EnterNames)
  }

  def isOKEnabled(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getButton(buttonOK).isEnabled
  }
}
