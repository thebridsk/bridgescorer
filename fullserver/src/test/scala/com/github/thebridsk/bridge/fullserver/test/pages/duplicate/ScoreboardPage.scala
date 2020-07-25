package com.github.thebridsk.bridge.fullserver.test.pages.duplicate

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.utilities.logging.Logger

object ScoreboardPage {

  val log = Logger[ScoreboardPage]()

  case class PlaceEntry( place: Int, points: String, teams: List[Team] )

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (did, view) = findDuplicateId
    new ScoreboardPage( Option( did ), view)
  }

  def waitFor(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (did, view) = eventually { findDuplicateId }
    new ScoreboardPage( Option( did ), view)
  }

  def goto( id: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to getUrl(id)
    new ScoreboardPage( Option(id), CompletedViewType )
  }

  def getUrl( id: String ) = {
    TestServer.getAppPageUrl(s"duplicate/match/${id}")
  }

  val buttonIdsCompleted =
                 ("Table_1", "Table 1") ::
                 ("Table_2", "Table 2") ::
                 Nil

  val buttonIdsDirector =
                 ("AllBoards", "All Boards") ::
                 ("Game", "Completed Games Scoreboard") ::
                 ("EditNames", "Edit Names") ::
                 ("Delete", "Delete") ::
                 Nil

  def buttonIdsTable( n: String ) =
                 ("Table", s"Table ${n}") ::
                 ("Game", "Completed Games Scoreboard") ::
                 ("AllBoards", "All Boards") ::
                 Nil

  def buttonIds( view: ViewType ) = view match {
    case DirectorViewType => buttonIdsDirector
    case CompletedViewType => buttonIdsCompleted
    case TableViewType(tid,rid) => buttonIdsTable(tid)
  }

  private val patternComplete = """(M\d+)(?:/([^/]+)/([a-zA-Z]\d+))?""".r
  private val patternDirector = """(M\d+)/director(?:/([^/]+)/([a-zA-Z]\d+))?""".r
  private val patternTable = """(M\d+)/table/(\d+)/round/(\d+)/([^/]+)(?:/([a-zA-Z]\d+))?""".r

  /**
   * Get the duplicate ID, the View type, subresource name and id.
   * currentUrl needs to be one of the following:
   *   duplicate/dupid[/sub/id]
   *   duplicate/dupid/director[/sub/id]
   *   duplicate/dupid/table/tableid/round/roundid[/game|/sub/id]
   * @return Tuple4(
   *            dupid
   *            ViewType   - CompletedViewType, DirectorViewType, TableViewType(tableid,roundid)
   *            subresource    - "game" or subresource
   *            Option(subid)
   *          )
   */
  def findIds(implicit webDriver: WebDriver, pos: Position): (String, String, ViewType, Option[String], Option[String]) = {
    val prefix = TestServer.getAppPageUrl("duplicate/match/")
    val cur = currentUrl
    withClue(s"Unable to determine duplicate id: ${cur}") {
      cur must startWith (prefix)
      cur.drop( prefix.length() ) match {
        case patternComplete(did,subres,subid) => (cur,did,CompletedViewType, Option(subres), Option(subid))
        case patternDirector(did,subres,subid) => (cur,did,DirectorViewType, Option(subres), Option(subid))
        case patternTable(did,tid,rid,subres,subid) => (cur,did,TableViewType(tid,rid), Option(if (subres=="game") null else subres), Option(subid))
        case _ => fail("Could not determine view type")
      }
    }
  }


  /**
   * Get the duplicate ID and the View type.
   * currentUrl needs to be one of the following:
   *   duplicate/dupid
   *   duplicate/dupid/director
   *   duplicate/dupid/table/tableid/round/roundid/game
   * @return Tuple2(
   *            dupid
   *            ViewType   - CompletedViewType, DirectorViewType, TableViewType(tableid,roundid)
   *          )
   */
  def findDuplicateId(implicit webDriver: WebDriver, pos: Position): (String, ViewType) = {
    val (curr,dupid,viewtype,subres,subid) = findIds
    withClue(s"Current URL must is not a scoreboard url: $curr") {
      subres mustBe None
    }
    (dupid,viewtype)
  }

  sealed trait ViewType

  object CompletedViewType extends ViewType
  object DirectorViewType extends ViewType
  case class TableViewType( table: String, round: String ) extends ViewType


  sealed trait ScoreStyle {
    val name: String
    override
    def toString() = getClass.getSimpleName
  }

  sealed trait ScoreStylePlayed extends ScoreStyle

  object ScoreStyleIMP extends ScoreStyle { val name = "International Match Points" }
  object ScoreStyleMP extends ScoreStyle { val name = "Match Points" }
  object ScoreStylePlayedMultiple extends ScoreStylePlayed { val name = "Played Scoring Method" }
  object ScoreStylePlayedMP extends ScoreStylePlayed { val name = "Played Scoring Method: MP" }
  object ScoreStylePlayedIMP extends ScoreStylePlayed { val name = "Played Scoring Method: IMP" }
  object ScoreStylePlayedUnknown extends ScoreStylePlayed { val name = "Played Scoring Method: Unknown" }

  val validScoreStyles =
    ScoreStyleMP::
    ScoreStyleIMP::
    ScoreStylePlayedMultiple::
    ScoreStylePlayedMP::
    ScoreStylePlayedIMP::
    ScoreStylePlayedUnknown::
    Nil

  /**
   * @param style the input style, valid values are from ScoreStyle sealed trait
   * @return Some(style) if successful, otherwise returns current input style
   */
  def setScoreStyle( style: ScoreStyle )( implicit webDriver: WebDriver, pos: Position ): Option[ScoreStyle] = {
    if (!validScoreStyles.contains(style)) fail(s"""Specified style, ${style} is not valid, must be one of ${validScoreStyles.mkString(", ")}""")
    val stop = Some(style)
    var last: Option[ScoreStyle] = None
    for ( i <- 1 to 3 ) {
      last = getScoreStyle
      if (last == stop) return stop
      click on id("ScoreStyle")
    }
    last
  }

  def getScoreStyle(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Option[ScoreStyle] = {
    val cur = find(id("ScoreStyle")).text
    validScoreStyles.find( s => s.name == cur)
  }
}

import ScoreboardPage._
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.TablePage.EnterNames
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.Popup

class ScoreboardPage(
                      val dupid: Option[String] = None,
                      val view: ScoreboardPage.ViewType = ScoreboardPage.CompletedViewType
                    )( implicit
                        val webDriver: WebDriver,
                        pageCreated: SourcePosition
                    ) extends Page[ScoreboardPage] with Popup[ScoreboardPage] with PageWithBoardButtons {

  private def validateInternal(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {
    val (did,viewFromUrl) = findDuplicateId

    dupid match {
      case Some(oid) => oid mustBe did
      case None =>
    }

    viewFromUrl mustBe view

    // <div class="dupDivScoreboardHelp"><h1>Table 1 Scoreboard, Round 1, Teams 3 and 4</h1>
    // <div class="dupDivScoreboardHelp"><h1>Completed Games Scoreboard</h1>
    // <div class="dupDivScoreboardHelp"><h1>Director's Scoreboard</h1>

    val help = findElemByXPath("//div[contains(concat(' ', @class, ' '), ' dupDivScoreboardHelp ')]/h1").text
    view match {
      case ScoreboardPage.CompletedViewType =>
        help mustBe "Completed Games Scoreboard"
      case ScoreboardPage.DirectorViewType =>
        help mustBe "Director's Scoreboard"
      case ScoreboardPage.TableViewType(table,round) =>
        help must fullyMatch regex (s"""Table $table Scoreboard, Round $round, Teams .+ and .+""")
    }
    did
  }}( Position.here )

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
    log.fine(s"ScoreboardPage.validate: patienceConfig=${patienceConfig}, pos=${pos}")
    eventually {
      log.fine(s"ScoreboardPage.validate: starting validate method, patienceConfig=${patienceConfig}, pos=${pos}")
      val did = validateInternal

      val buttons = eventually{ findButtons( Map( buttonIds(view):_* ) ) }
      val sb = new ScoreboardPage( Option(did), view )
      sb
    }
  }( Position.here )

  def validate(boards: List[Int])(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
    eventually {
      logMethod(s"${pos.line} ${getClass.getSimpleName}.validate inside eventually") {
        val did = validateInternal

        val ids = buttonIds(view) :::
                  (for (i <- boards) yield {
                    ("Board_B"+i, i.toString() )
                  }).toList

        val buttons = findButtons( Map( ids:_* ) )
        new ScoreboardPage( Option(did) )
      }
    }
  }

  def clickHome(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Home")
    new HomePage
  }

  def clickMainMenu(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("MainMenu")
    this
  }

  def validateMainMenu(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      findElemById("Summary")
    }
    this
  }

  def clickHelpMenu(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("HelpMenu")
    this
  }

  def validateHelpMenu(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      findElemById("Help")
    }
    this
  }

  def clickDirectorButton(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (view != CompletedViewType) fail("Must be in Completed view to hit director button")
    clickButton("Director")
    new ScoreboardPage( dupid, DirectorViewType )
  }

  def clickScoreStyle(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("ScoreStyle")
    this
  }

  def clickCompleteGameButton(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (!view.isInstanceOf[TableViewType]) fail("Must be in Table view to hit complete game button")
    clickButton("Game")
    new ScoreboardPage( dupid, CompletedViewType )
  }

  def clickAllBoards(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("AllBoards")
    new BoardsPage
  }

  def clickEditNames(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("EditNames")
    new EditNamesPage( dupid.get )
  }

  def clickTableButton(table: Int)(implicit patienceConfig: PatienceConfig, pos: Position) = {
    view match {
      case DirectorViewType => fail("Must be in Completed view to hit table button")
      case CompletedViewType =>
        clickButton(s"Table_${table}")
        new TablePage( dupid.get, table.toString(), EnterNames )
      case TableViewType(tid,rid) =>
        val e = getButton("Table")
        e.text mustBe s"Table ${table}"
        clickButton(s"Table")
        new TablePage( dupid.get, table.toString(), EnterNames )
    }
  }

  def checkTableButton( table: String )(implicit pos: Position) = {
    view match {
      case DirectorViewType =>
        fail("Must be in Completed view to hit table button")
      case CompletedViewType =>
        getButton(s"Table_${table}", Some(s"Table ${table}") ) mustBe Symbol("Enabled")
      case TableViewType(tid,rid) =>
        getButton("Table", Some(s"Table ${table}")) mustBe Symbol("Enabled")
    }
  }

  def clickBoardButton( board: Int )(implicit patienceConfig: PatienceConfig, pos: Position): HandPage =
    clickBoardToHand(board)

  def clickBoardToHand( board: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(s"Board_B${board}")
    new HandPage
  }

  def clickBoardToBoard( board: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(s"Board_B${board}")
    new BoardPage
  }

  def clickSummary(implicit patienceConfig: PatienceConfig, pos: Position) = {
//    clickButton("Summary")
    val b = findElemById("Summary")
    b.enter
    new ListDuplicatePage(None)
  }

  /**
   * @return a List of Lists.  The outer list is the rows (teams) in the table, the inner list are the columns.
   * The columns are: teamNumber, players, totals, board1, ...
   */
  def getTable(implicit pos: Position) = {
    val boards = getElemsByXPath("""//table[@id='scoreboard']/thead/tr[2]/th""").size
    getElemsByXPath("""//table[@id='scoreboard']/tbody/tr/td""").map(c=>c.text).grouped(boards+3).toList
  }

  /**
   * @return a List of Lists.  The outer list is the rows (place) in the table, the inner list are the columns.
   * The columns are: place, points, players
   */
  def getPlaceTable(implicit pos: Position) = {
    getElemsByXPath("""//table[@id='scoreboardplayers']/tbody/tr/td""").map(c=>c.text).grouped(3).toList
  }

  /**
   * @param team
   */
  def checkTable( teams: TeamScoreboard* /* ( Team, String, List[String] )* */ )(implicit pos: Position) = {
    val table = getTable
    teams.foreach{ case TeamScoreboard( players, points, total, boardscores ) =>
      withClue(s"""checking scoreboard row for ${players} total ${total} boards ${boardscores}""") {
        table.find( row => row(0)==players.teamid.toString ) match {
          case Some(row) =>
            row(1) mustBe players.toStringForScoreboard
            row(2) mustBe total
            row.drop(3) mustBe boardscores
          case None =>
            fail(s"""${pos.line}: Did not find team ${players.teamid}""" )
        }
      }
    }
  }

  /**
   * @param team a Tuple4( teamnumber, players, points, boardscores )
   */
  def checkPlaceTable( places: PlaceEntry* )(implicit pos: Position) = {
    val table = getPlaceTable
    places.foreach{ case PlaceEntry( place, points, teams ) =>
      withClue( s"""working with place ${place} points ${points} teams ${teams}""" ) {
        table.find( row => row(0)==place.toString() ) match {
          case Some(row) =>
            row(1) mustBe points
            val players = row(2)
            teams.foreach(team => players must include (team.toStringForPlayers) )
          case None =>
            fail(s"""${pos.line}: Did not find place $place""" )
        }
      }
    }
  }

  def clickDelete(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      clickButton("Delete")
    }
    this
  }


  def clickDeleteOk(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickPopUpOk
    // Thread.sleep(200L)
    ListDuplicatePage.waitFor
  }

  /**
   * @param style the input style, valid values are from ScoreStyle sealed trait
   */
  def setScoreStyle( style: ScoreStyle )( implicit webDriver: WebDriver, pos: Position ) = {
    ScoreboardPage.setScoreStyle(style)
    this
  }

  def getScoreStyle(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Option[ScoreStyle] = {
    ScoreboardPage.getScoreStyle
  }

}
