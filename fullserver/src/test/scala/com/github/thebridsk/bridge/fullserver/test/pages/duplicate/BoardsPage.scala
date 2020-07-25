package com.github.thebridsk.bridge.fullserver.test.pages.duplicate

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually.{ patienceConfig => _, _ }
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage.TableViewType
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage.ViewType
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.bridge.MadeOrDown
import com.github.thebridsk.bridge.data.bridge.Made
import com.github.thebridsk.bridge.data.bridge.Down
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.data.bridge.ContractSuit
import com.github.thebridsk.bridge.data.bridge.ContractDoubled
import com.github.thebridsk.bridge.data.bridge.Vulnerability
import com.github.thebridsk.bridge.data.bridge.Vul
import com.github.thebridsk.bridge.data.bridge.NotVul
import com.github.thebridsk.browserpages.PageBrowser
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage

object BoardsPage {

  val log = Logger[BoardsPage]()

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new BoardsPage
  }

  def goto(dupid: String, director: Boolean )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor(dupid, director)
    new BoardsPage
  }

  def goto(dupid: String, tableId: Int, roundId: Int )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor(dupid, tableId, roundId)
    new BoardsPage
  }

  def urlFor(dupid: String, tableId: Int, roundId: Int ) =
    TestServer.getAppPageUrl(s"duplicate/${dupid}/table/${tableId}/round/${roundId}/boards")

  def urlFor(dupid: String, director: Boolean ) = {
    val dir = if (director) "director/" else ""
    TestServer.getAppPageUrl(s"duplicate/match/${dupid}/${dir}boards")
  }

  /**
   * Get the duplicate ID and the View type and board ID.
   * @return Tuple3(
   *            dupid
   *            ViewType
   *            boardid
   *          )
   */
  def findIds(implicit webDriver: WebDriver, pos: Position): (String, ViewType) = {
    ScoreboardPage.findIds match {
      case ( cururl, dupid, viewtype, Some("boards"), None) => (dupid,viewtype)
      case (cururl,_,_,_,_) =>
        fail(s"""${pos.line} Unable to determine Ids from Board page: ${cururl}""")
    }
  }

  def toPointsString( pts: Double ) = {
    val ipts = pts.floor.toInt
    val fpts = pts - ipts
    val fract = if (fpts < 0.01) "" else Strings.half   // 1/2
    if (ipts == 0 && !fract.isEmpty()) fract else ipts.toString+fract
  }

  val patternToBoardId = """Board_B(\d+)""".r

  def elemIdToBoardId( elemid: String ) = elemid match {
    case patternToBoardId(bid) => Some( bid.toInt )
    case _ => None
  }
}

class BoardsPage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[BoardsPage] {
  import BoardsPage._

  def getTableId(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    val (dupid, viewtype) = findIds
    viewtype match {
      case TableViewType(tid,rid) =>
        Some(tid)
      case _ =>
        None
    }
  }

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    findButtons("Game", "Game2")
    this
  }

  def checkHandButtons( board: Int, hands: Int* )(implicit patienceConfig: PatienceConfig, pos: Position): this.type = eventually {
    val buttons = findElemsByXPath( s"""//table[@id='Board_B${board}']//button""").map(e => e.text)
    withClue( s"""On board ${board}""") {
      buttons must contain theSameElementsAs hands.map(id => s"""Hand_T${id}""")
    }
    this
  }

  private def getTeamRow( board: Int, team: Int )(implicit pos: Position) = {
    findElemsByXPath(s"""//table[@id='Board_B${board}']/tbody/tr[${team}]/td""")
  }

  def checkTeamNSScore( board: Int,
                        team: Int,
                        contract: String,
                        dec: PlayerPosition,
                        madeOrDown: MadeOrDown,
                        tricks: Int,
                        nsScore: Int,
                        ewTeam: Int,
                        matchPoints: Double
                      )(implicit pos: Position) = {
    withClue(s"checking NS score on board ${board} for team ${team}") {
      val row = getTeamRow(board,team)
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

  def checkTeamEWScore( board: Int, team: Int, ewScore: Int, matchPoints: Double )(implicit pos: Position) = {
    withClue(s"checking EW score on board ${board} for team ${team}") {
      val row = getTeamRow(board,team)
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
      board: Int,
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
    ): BoardsPage = {
    val v = vul match {
      case Vul => " Vul"
      case NotVul => ""
    }
    val contract = if (contractTricks == 0) "Passed Out"
    else s"""${contractTricks}${contractSuit.suit}${contractDoubled.forScore}${v}"""
    checkTeamNSScore(board,nsTeam, contract, declarer, madeOrDown, tricks, nsScore, ewTeam, nsMP)
    checkTeamEWScore(board,ewTeam, -nsScore, ewMP)
    this
  }

  def checkTeamScores( board: Int, eh: EnterHand ): BoardsPage = {
    import eh._
    checkTeamScores(board,nsTeam, nsScore, nsMP, ewTeam, ewMP, contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks, vul)
  }

  def checkTeamNSPlayed( board: Int, team: Int, ewTeam: Int )(implicit pos: Position) = {
    withClue(s"checking NS score on board ${board} for played for team ${team}") {
      val row = getTeamRow(board,team)
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

  def checkTeamEWPlayed( board: Int, team: Int )(implicit pos: Position) = {
    withClue(s"checking EW score on board ${board} for played for team ${team}") {
      val row = getTeamRow(board,team)
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

  def checkTeamNotPlayed( board: Int, nsTeam: Int, ewTeam: Int )(implicit pos: Position) = {
    checkTeamNSNotPlayed(board,nsTeam, ewTeam)
    checkTeamEWNotPlayed(board,ewTeam)
  }

  def checkTeamPlayed( board: Int, nsTeam: Int, ewTeam: Int )(implicit pos: Position) = {
    checkTeamNSPlayed(board,nsTeam, ewTeam)
    checkTeamEWPlayed(board,ewTeam)
  }

  def checkTeamNSNotPlayed( board: Int, team: Int, ewTeam: Int )(implicit pos: Position) = {
    withClue(s"checking NS score on board ${board} for not played for team ${team}") {
      val row = getTeamRow(board,team)
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

  def checkTeamNeverPlays( board: Int, team: Int )(implicit pos: Position) = {
    import BoardPage._
    withClue(s"checking for never play for team ${team}") {
      val row = getTeamRow(board,team)
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

  def checkTeamEWNotPlayed( board: Int, team: Int )(implicit pos: Position) = {
    withClue(s"checking EW score on board ${board} for not played for team ${team}") {
      val row = getTeamRow(board,team)
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

  def checkOthers( b: HandsOnBoard, allhands: AllHandsInMatch, checkmarks: Boolean = false )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val allplayed = b.other.find(oh => oh.isInstanceOf[OtherHandNotPlayed]).isEmpty
    b.other.foreach{ oh =>
      oh match {
        case OtherHandNotPlayed(tableid,roundid,boardid) =>
          val ob = allhands.getBoard(tableid, roundid, boardid)
          checkTeamNotPlayed(b.board, ob.hand.nsTeam, ob.hand.ewTeam)
        case OtherHandPlayed(tableid,roundid,boardid,nsMP,ewMP,nsIMP,ewIMP) =>
          val ob = allhands.getBoard(tableid, roundid, boardid)
          val eh = ob.hand.copy( nsMP=nsMP, ewMP=ewMP)
          if (!allplayed && checkmarks) {
            checkTeamPlayed(b.board, eh.nsTeam, eh.ewTeam)
          } else {
            checkTeamScores(b.board, eh)
          }
        case TeamNotPlayingHand(board,team) =>
          checkTeamNeverPlays(board, team.teamid)
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
    withClue( s"""${pos.line} BoardsPage.checkHand round ${round} board ${board} checkmarks ${checkmarks}""" ) {
      allhands.getBoardToRound(round, board) match {
        case Some(b) =>
          val allplayed = b.other.find(oh => oh.isInstanceOf[OtherHandNotPlayed]).isEmpty
          if (!allplayed && checkmarks) {
            checkTeamPlayed(board,b.hand.nsTeam, b.hand.ewTeam)
          } else {
            checkTeamScores(board,b.hand)
          }
          checkOthers(b, allhands, checkmarks)
        case None =>
          val maxRound = allhands.rounds.last
          allhands.getBoardToRound(maxRound, board) match {
            case Some(b) =>
              val allplayed = b.other.find(oh => oh.isInstanceOf[OtherHandNotPlayed]).isEmpty
              checkTeamNotPlayed(board,b.hand.nsTeam, b.hand.ewTeam)
              def checkNotPlayed( table: Int, round: Int, brd: Int ) = {
                val hob = allhands.getBoard(table, round, brd)
                checkTeamNotPlayed(board,hob.hand.nsTeam, hob.hand.ewTeam)
              }
              b.other.foreach( oh => oh match {
                case OtherHandNotPlayed( table, round, board) =>
                  checkNotPlayed(table,round,board)
                case OtherHandPlayed(table, round, board, nsMP, ewMP,nsIMP,ewIMP) =>
                  checkNotPlayed(table,round,board)
                case TeamNotPlayingHand(board,team) =>
                  checkTeamNeverPlays(board, team.teamid)
              })
            case None =>

          }
      }
      this
    }
  }

  def getBoardIds(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    findElemsByXPath(HomePage.divBridgeAppPrefix+"""//table""").flatMap(e => elemIdToBoardId(e.attribute("id").get) match {
      case Some(id) => id::Nil
      case None => Nil
    } ).sorted
  }

  def clickHand( board: Int, hand: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    findElemByXPath(s"""//table[@id='Board_B${board}']//button[@id='Hand_T${hand}']""").click
    new HandPage
  }

  def clickScoreboard(implicit patienceConfig: PatienceConfig, pos: Position) = {
    PageBrowser.scrollToTop
//    Thread.sleep(10L)
    clickButton("Game")
    ScoreboardPage.waitFor
  }

}
