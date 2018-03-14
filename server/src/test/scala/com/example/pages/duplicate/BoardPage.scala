package com.example.pages.duplicate

import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually.{ patienceConfig => _, _ }
import org.scalatest.MustMatchers._
import com.example.test.selenium.TestServer
import utils.logging.Logger
import com.example.pages.Page
import com.example.pages.PageBrowser._
import com.example.pages.duplicate.ScoreboardPage.TableViewType
import com.example.pages.duplicate.ScoreboardPage.ViewType
import com.example.pages.duplicate.ScoreboardPage.CompletedViewType
import com.example.pages.duplicate.ScoreboardPage.DirectorViewType
import com.example.data.bridge.PlayerPosition
import com.example.data.bridge.MadeOrDown
import com.example.data.bridge.Made
import com.example.data.bridge.Down
import com.example.data.util.Strings
import com.example.pages.duplicate.TablePage.Hands
import com.example.data.bridge.ContractSuit
import com.example.data.bridge.ContractDoubled
import com.example.data.bridge.Vulnerability
import com.example.data.bridge.Vul
import com.example.data.bridge.NotVul
import com.example.pages.Element

object BoardPage {

  val log = Logger[BoardPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new BoardPage
  }

  def goto(dupid: String, director: Boolean, boardId: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor(dupid, director, boardId)
    new BoardPage
  }

  def goto(dupid: String, tableId: Int, roundId: Int, boardId: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor(dupid, tableId, roundId, boardId)
    new BoardPage
  }

  def urlFor(dupid: String, tableId: Int, roundId: Int, boardId: String ) =
    TestServer.getAppPageUrl(s"duplicate/${dupid}/table/${tableId}/round/${roundId}/boards/${boardId}")

  def urlFor(dupid: String, director: Boolean, boardId: String ) = {
    val dir = if (director) "director/" else ""
    TestServer.getAppPageUrl(s"duplicate/${dupid}/${dir}boards/${boardId}")
  }

  /**
   * Get the duplicate ID and the View type and board ID.
   * @return Tuple3(
   *            dupid
   *            ViewType
   *            boardid
   *          )
   */
  def findIds(implicit webDriver: WebDriver, pos: Position): (String, ViewType, String) = {
    ScoreboardPage.findIds match {
      case ( dupid, viewtype, Some("boards"), Some(boardid) ) => (dupid,viewtype,boardid)
      case x =>
        fail(s"""${pos.line} Unable to determine Ids from Board page: ${currentUrl}""")
    }
  }

  def toPointsString( pts: Double ) = {
    val ipts = pts.floor.toInt
    val fpts = pts - ipts
    val fract = if (fpts < 0.01) "" else Strings.half   // 1/2
    if (ipts == 0 && !fract.isEmpty()) fract else ipts.toString+fract
  }

  implicit class ElementWrapper( val e: Element ) extends AnyVal {
    def isGray = {
      s""" ${e.attribute("class").get} """.indexOf(" dupTableCellGray ") >= 0
    }
    def isWhite = {
      s""" ${e.attribute("class").get} """.indexOf(" dupTableCellGray ") < 0
    }
  }

  // for director's view or completed view
  val ColorSelected = "baseButtonSelected"
  val ColorPlayed = "baseRequired"
  val ColorPartiallyPlayed = "baseRequiredNotNext"

  // for table view
  val ColorTablePlayed = "baseRequiredNotNext"

}

class BoardPage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[BoardPage] {
  import BoardPage._

  def getTableId(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    val (dupid, viewtype, bid) = findIds
    viewtype match {
      case TableViewType(tid,rid) =>
        Some(tid)
      case _ =>
        None
    }
  }

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    val buttons = findAllButtons
    buttons must contain key ("Game")
    getTableId match {
      case Some(tid) =>
        buttons("Table").text mustBe s"Table ${tid}"
      case _ =>
    }
    this
  }

  def checkBoardButtons( boards: Int* )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = eventually {
    val buttons = findAllButtons
    boards.foreach( id => buttons must contain key (s"""Board_B${id}"""))
    this
  }

  def checkHandButtons( hands: Int* )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = eventually {
    val buttons = findAllButtons
    hands.foreach( id => buttons must contain key (s"""Hand_T${id}"""))
    val allhandbuttons = buttons.keys.filter( k => k.startsWith("Hand_T")).toList
    withClue(s"""Looking for ${hands}, found ${allhandbuttons}""") {
      allhandbuttons.size mustBe hands.size
    }
    this
  }

  /**
   * Check if the specified buttons are in the played or notplayed section.
   * This is when the board is displayed with the table view.
   * @param played
   * @param boards
   * @throws TestFailedException
   */
  def checkBoardButtons( current: Int, played: Boolean, boards: Int* )(implicit pos: Position): this.type = eventually {
    val bs = boards.map(id => s"""Board_B${id}""")
    val buttonsOnPage = findBoardButtons(played).map { b =>
      (b,b.attribute("id").get)
    }
    buttonsOnPage.map(e=>e._2) must contain allElementsOf(bs)
    val currentid = s"""Board_B${current}"""
    buttonsOnPage.foreach { case (el,id) =>
      if (id == currentid) {
        checkBoardButtonSelected(current)
      } else {
        if (played) {
          checkBoardButtonIdNoColor(id)
        } else {
          checkBoardButtonIdColor(ColorTablePlayed,id)
        }
      }
    }
    this
  }

  /**
   * Check the board button colors.
   * This is when the board is displayed with the director's view or completed view
   * @param current
   * @param noplayed
   * @param played
   * @param partiallyplayed
   * @throws TestFailedException
   */
  def checkBoardButtonColors( current: Int, notplayed: List[Int], played: List[Int], partiallyplayed: List[Int] )
                            (implicit pos: Position): this.type = eventually {
    checkBoardButtonSelected(current)
    checkBoardButtonNoColor(notplayed:_*)
    checkBoardButtonColor(ColorPartiallyPlayed, partiallyplayed.filter( id => id!=current):_*)
    checkBoardButtonColor(ColorPlayed, played.filter( id => id!=current):_*)
  }

  /**
   * @param color the style that is specifying the color.
   *              values:
   *                baseButtonSelected
   *                baseRequired
   *                baseRequiredNotNext
   * @param board
   * @throws TestFailedException
   */
  private def checkBoardButtonColor( color: String, boards: Int*)
                                   ( implicit pos: Position): this.type = eventually {
    boards.foreach { board =>
      val bs = s"""Board_B${board}"""
      val elem = find( xpath( s"""//button[@id='${bs}']""" ))
      withClue( s"""Button ${bs} has classes ${elem.attribute("class")}: expecting ${color}""" ) {
        elem.containsClass(color) mustBe true
      }
    }
    this
  }

  /**
   * @param color the style that is specifying the color.
   *              values:
   *                baseButtonSelected
   *                baseRequired
   *                baseRequiredNotNext
   * @param board
   * @throws TestFailedException
   */
  private def checkBoardButtonIdColor( color: String, boards: String*)
                                   ( implicit pos: Position): this.type = eventually {
    boards.foreach { board =>
      val elem = find( xpath( s"""//button[@id='${board}']""" ))
      withClue( s"""Button ${board} has classes ${elem.attribute("class")}: expecting ${color}""" ) {
        elem.containsClass(color) mustBe true
      }
    }
    this
  }

  /**
   * @param board
   * @throws TestFailedException
   */
  private def checkBoardButtonNoColor( boards: Int* )
                                     ( implicit pos: Position): this.type = eventually {
    boards.foreach { board =>
      val bs = s"""Board_B${board}"""
      val elem = find( xpath( s"""//button[@id='${bs}']""" ))
      (ColorSelected::ColorPlayed::ColorPartiallyPlayed::Nil).foreach { color =>
        withClue( s"""Button ${bs} has classes ${elem.attribute("class")}: expecting to not find ${color}""" ) {
          elem.containsClass(color) mustBe false
        }
      }
    }
    this
  }

  /**
   * @param board
   * @throws TestFailedException
   */
  private def checkBoardButtonIdNoColor( boards: String* )
                                     ( implicit pos: Position): this.type = eventually {
    boards.foreach { board =>
      val elem = find( xpath( s"""//button[@id='${board}']""" ))
      (ColorSelected::ColorPlayed::ColorPartiallyPlayed::Nil).foreach { color =>
        withClue( s"""Button ${board} has classes ${elem.attribute("class")}: expecting to not find ${color}""" ) {
          elem.containsClass(color) mustBe false
        }
      }
    }
    this
  }

  /**
   * @param board
   * @throws TestFailedException
   */
  def checkBoardButtonSelected( board: Int )(implicit pos: Position): this.type =
    checkBoardButtonColor(ColorSelected, board)

  def findBoardButtons( played: Boolean )(implicit pos: Position) = {
    val p = if (played) "Played: " else "Unplayed: "
    val xp = s"""//span[b="${p}"]/button"""
    findElemsByXPath(xp)
  }

  private def getTeamRow( team: Int )(implicit pos: Position) = {
    findElemsByXPath(s"""//table/tbody/tr[${team}]/td""")
  }

  def checkTeamNSScore( team: Int,
                        contract: String,
                        dec: PlayerPosition,
                        madeOrDown: MadeOrDown,
                        tricks: Int,
                        nsScore: Int,
                        ewTeam: Int,
                        matchPoints: Double
                      )(implicit pos: Position) = {
    withClue(s"checking NS score for team ${team}") {
      val row = getTeamRow(team)
      row(0).text mustBe team.toString()
      row(1).text mustBe contract
      if (contract == "Passed Out") {
        row(2).text mustBe "-"
        row(3).text mustBe "-"
        row(4).text mustBe "-"
      } else {
        row(2).text mustBe dec.pos
        madeOrDown match {
          case Made =>
            row(3).text mustBe tricks.toString()
            row(4).text mustBe ""
          case Down =>
            row(3).text mustBe ""
            row(4).text mustBe tricks.toString()
        }
      }
      row(5).text mustBe nsScore.toString()
      row(6).text mustBe ""
      row(7).text mustBe ewTeam.toString()
      row(8).text mustBe toPointsString(matchPoints)
      this
    }
  }

  def checkTeamEWScore( team: Int, ewScore: Int, matchPoints: Double )(implicit pos: Position) = {
    withClue(s"checking EW score for team ${team}") {
      val row = getTeamRow(team)
      row(0).text mustBe team.toString()
      row(1).text mustBe ""
      row(2).text mustBe ""
      row(3).text mustBe ""
      row(4).text mustBe ""
      row(5).text mustBe ""
      row(6).text mustBe ewScore.toString()
      row(7).text mustBe ""
      row(8).text mustBe toPointsString(matchPoints)
      this
    }
  }

  def checkTeamScores(
      nsTeam: Int,
      nsScore: Int,
      nsMP: Double,
      ewTeam: Int,
      ewMP: Double,
      contractTricks: Int,
      contractSuit: ContractSuit,
      contractDoubled: ContractDoubled,
      declarer: PlayerPosition,
      madeOrDown: MadeOrDown,
      tricks: Int,
      vul: Vulnerability
    )(implicit
        patienceConfig: PatienceConfig,
        pos: Position
    ): BoardPage = {
    val v = vul match {
      case Vul => " Vul"
      case NotVul => ""
    }
    val contract = if (contractTricks == 0) "Passed Out"
    else s"""${contractTricks}${contractSuit.suit}${contractDoubled.forScore}${v}"""
    checkTeamNSScore(nsTeam, contract, declarer, madeOrDown, tricks, nsScore, ewTeam, nsMP)
    checkTeamEWScore(ewTeam, -nsScore, ewMP)
    this
  }

  def checkTeamScores( eh: EnterHand ): BoardPage = {
    import eh._
    checkTeamScores(nsTeam, nsScore, nsMP, ewTeam, ewMP, contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks, vul)
  }

  def checkTeamNSPlayed( team: Int, ewTeam: Int )(implicit pos: Position) = {
    withClue(s"checking NS score for played for team ${team}") {
      val row = getTeamRow(team)
      row(0).text mustBe team.toString()
      row(1).text mustBe "played"
      row(2).text mustBe Strings.checkmark
      row(3).text mustBe Strings.checkmark
      row(4).text mustBe Strings.checkmark
      row(5).text mustBe Strings.checkmark
      row(6).text mustBe ""
      row(7).text mustBe ewTeam.toString()
      row(8).text mustBe Strings.checkmark
      this
    }
  }

  def checkTeamEWPlayed( team: Int )(implicit pos: Position) = {
    withClue(s"checking EW score for played for team ${team}") {
      val row = getTeamRow(team)
      row(0).text mustBe team.toString()
      row(1).text mustBe "played"
      row(2).text mustBe ""
      row(3).text mustBe ""
      row(4).text mustBe ""
      row(5).text mustBe ""
      row(6).text mustBe Strings.checkmark
      row(7).text mustBe ""
      row(8).text mustBe Strings.checkmark
      this
    }
  }

  def checkTeamNotPlaying( team: Int )(implicit pos: Position) = {
    withClue(s"checking for team ${team} not playing board") {
      val row = getTeamRow(team)
      row(0).text mustBe team.toString()
      row(1).text mustBe ""
      row(2).text mustBe ""
      row(3).text mustBe ""
      row(4).text mustBe ""
      row(5).text mustBe ""
      row(6).text mustBe ""
      row(7).text mustBe ""
      row(8).text mustBe ""
      this
    }
  }

  def checkTeamNotPlayed( nsTeam: Int, ewTeam: Int )(implicit pos: Position) = {
    checkTeamNSNotPlayed(nsTeam, ewTeam)
    checkTeamEWNotPlayed(ewTeam)
  }

  def checkTeamPlayed( nsTeam: Int, ewTeam: Int )(implicit pos: Position) = {
    checkTeamNSPlayed(nsTeam, ewTeam)
    checkTeamEWPlayed(ewTeam)
  }

  def checkTeamNSNotPlayed( team: Int, ewTeam: Int )(implicit pos: Position) = {
    withClue(s"checking NS score for not played for team ${team}") {
      val row = getTeamRow(team)
      row(0).text mustBe team.toString()
      row(1).text mustBe ""
      row(2).text mustBe ""
      row(3).text mustBe ""
      row(4).text mustBe ""
      row(5).text mustBe ""
      row(6).text mustBe ""
      row(7).text mustBe ewTeam.toString()
      row(8).text mustBe ""
      this
    }
  }

  def checkTeamEWNotPlayed( team: Int )(implicit pos: Position) = {
    withClue(s"checking EW score for not played for team ${team}") {
      val row = getTeamRow(team)
      row(0).text mustBe team.toString()
      row(1).text mustBe ""
      row(2).text mustBe ""
      row(3).text mustBe ""
      row(4).text mustBe ""
      row(5).text mustBe ""
      row(6).text mustBe ""
      row(7).text mustBe ""
      row(8).text mustBe ""
      row(5).isGray mustBe true    // NS
      row(6).isWhite mustBe true    // EW
      this
    }
  }

  def checkTeamNeverPlays( team: Int )(implicit pos: Position) = {
    withClue(s"checking NS score for not played for team ${team}") {
      val row = getTeamRow(team)
      row(0).text mustBe team.toString()
      row(1).text mustBe ""
      row(2).text mustBe ""
      row(3).text mustBe ""
      row(4).text mustBe ""
      row(5).text mustBe ""
      row(6).text mustBe ""
      row(7).text mustBe ""
      row(8).text mustBe ""
      row(5).isGray mustBe true    // NS
      row(6).isGray mustBe true    // EW
      this
    }
  }

  def checkOthers( b: HandsOnBoard, allhands: AllHandsInMatch, checkmarks: Boolean = false )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val allplayed = b.other.find(oh => oh.isInstanceOf[OtherHandNotPlayed]).isEmpty
    b.other.foreach{ oh =>
      oh match {
        case OtherHandNotPlayed(tableid,roundid,boardid) =>
          val ob = allhands.getBoard(tableid, roundid, boardid)
          checkTeamNotPlayed(ob.hand.nsTeam, ob.hand.ewTeam)
        case OtherHandPlayed(tableid,roundid,boardid,nsMP,ewMP,nsIMP,ewIMP) =>
          val ob = allhands.getBoard(tableid, roundid, boardid)
          val eh = ob.hand.copy( nsMP=nsMP, ewMP=ewMP)
          if (!allplayed && checkmarks) {
            checkTeamPlayed(eh.nsTeam, eh.ewTeam)
          } else {
            checkTeamScores(eh)
          }
        case TeamNotPlayingHand( boardid, team ) =>
          checkTeamNeverPlays(team.teamid)
      }
    }
    this
  }

  def checkHand( round: Int,
                 board: Int,
                 allhands: AllHandsInMatch,
                 checkmarks: Boolean
               )(implicit
                   patienceConfig: PatienceConfig,
                   pos: Position
               ) = {
    withClue( s"""${pos.line} BoardPage.checkHand round ${round} board ${board} checkmarks ${checkmarks}""" ) {
      allhands.getBoardToRound(round, board) match {
        case Some(b) =>
          val allplayed = b.other.find(oh => oh.isInstanceOf[OtherHandNotPlayed]).isEmpty
          if (!allplayed && checkmarks) {
            checkTeamPlayed(b.hand.nsTeam, b.hand.ewTeam)
          } else {
            checkTeamScores(b.hand)
          }
          checkOthers(b, allhands, checkmarks)
        case None =>
          val maxRound = allhands.rounds.last
          allhands.getBoardToRound(maxRound, board) match {
            case Some(b) =>
              val allplayed = b.other.find(oh => oh.isInstanceOf[OtherHandNotPlayed]).isEmpty
              checkTeamNotPlayed(b.hand.nsTeam, b.hand.ewTeam)
              def checkNotPlayed( table: Int, round: Int, brd: Int ) = {
                val hob = allhands.getBoard(table, round, brd)
                checkTeamNotPlayed(hob.hand.nsTeam, hob.hand.ewTeam)
              }
              b.other.foreach( oh => oh match {
                case OtherHandNotPlayed( table, round, board) =>
                  checkNotPlayed(table,round,board)
                case OtherHandPlayed(table, round, board, nsMP, ewMP,nsIMP,ewIMP) =>
                  checkNotPlayed(table,round,board)
                case TeamNotPlayingHand(board,team) =>
                  checkTeamNeverPlays(team.teamid)
              })
            case None =>

          }
      }
      this
    }
  }

  def clickHand( hand: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    click on id(s"""Hand_T${hand}""")
    new HandPage
  }

  def clickPlayedBoard( board: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    click on id(s"""Board_B${board}""")
    findIds._3 mustBe s"""B${board}"""
    new BoardPage
  }

  def clickUnplayedBoard( board: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    click on id(s"""Board_B${board}""")
    new HandPage
  }

  def clickTableButton( table: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getTableId match {
      case Some(id) =>
        if (id != table.toString()) fail(s"""Not on board for table ${table}, on ${id}""")
        clickButton("Table")
        eventually { TablePage.current(Hands) }
      case None =>
        fail(s"""Not on board for table ${table}""")
    }
  }

  def clickScoreboard(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Game")
    ScoreboardPage.waitFor
  }

}
