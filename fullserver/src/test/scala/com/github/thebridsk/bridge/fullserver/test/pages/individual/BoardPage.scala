package com.github.thebridsk.bridge.fullserver.test.pages.individual

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually.{patienceConfig => _, _}
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.fullserver.test.pages.individual.ScoreboardPage.TableViewType
import com.github.thebridsk.bridge.fullserver.test.pages.individual.ScoreboardPage.ViewType
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.bridge.data.bridge.MadeOrDown
import com.github.thebridsk.bridge.data.bridge.Made
import com.github.thebridsk.bridge.data.bridge.Down
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.bridge.fullserver.test.pages.individual.TablePage.Hands
import com.github.thebridsk.bridge.data.bridge.ContractSuit
import com.github.thebridsk.bridge.data.bridge.ContractDoubled
import com.github.thebridsk.bridge.data.bridge.Vulnerability
import com.github.thebridsk.browserpages.Element
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HandPicture
import com.github.thebridsk.bridge.data.IndividualBoard

object BoardPage {

  val log: Logger = Logger[BoardPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardPage = {
    val (dupid, viewtype, boardid) = findIds
    new BoardPage(dupid, viewtype, boardid)
  }

  def goto(dupid: String, director: Boolean, boardId: String)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardPage = {
    go to urlFor(dupid, director, boardId)
    val viewtype = if (director) ScoreboardPage.DirectorViewType
                   else ScoreboardPage.CompletedViewType
    new BoardPage(dupid, viewtype, boardId)
  }

  def goto(dupid: String, tableId: Int, roundId: Int, boardId: String)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardPage = {
    go to urlFor(dupid, tableId, roundId, boardId)
    new BoardPage(dupid, TableViewType(tableId.toString(), roundId.toString()), boardId)
  }

  def urlFor(
      dupid: String,
      tableId: Int,
      roundId: Int,
      boardId: String
  ): String =
    TestServer.getAppPageUrl(
      s"individual/match/${dupid}/table/${tableId}/round/${roundId}/boards/${boardId}"
    )

  def urlFor(dupid: String, director: Boolean, boardId: String): String = {
    val dir = if (director) "director/" else ""
    TestServer.getAppPageUrl(
      s"individual/match/${dupid}/${dir}boards/${boardId}"
    )
  }

  /**
    * Get the duplicate ID and the View type and board ID.
    * @return Tuple3(
    *            dupid
    *            ViewType
    *            boardid
    *          )
    */
  def findIds(implicit
      webDriver: WebDriver,
      pos: Position
  ): (String, ViewType, String) = {
    ScoreboardPage.findIds match {
      case (cururl, dupid, viewtype, Some("boards"), Some(boardid)) =>
        (dupid, viewtype, boardid)
      case (cururl, _, _, _, _) =>
        fail(
          s"""${pos.line} Unable to determine Ids from Board page: ${cururl}"""
        )
    }
  }

  def toPointsString(pts: Double): String = {
    val ipts = pts.floor.toInt
    val fpts = pts - ipts
    val fract = if (fpts < 0.01) "" else Strings.half // 1/2
    if (ipts == 0 && !fract.isEmpty()) fract else ipts.toString + fract
  }

  implicit class ElementWrapper(private val e: Element) extends AnyVal {
    def isGray: Boolean = {
      s""" ${e.attribute("class").get} """.indexOf(" dupTableCellGray ") >= 0
    }
    def isWhite: Boolean = {
      s""" ${e.attribute("class").get} """.indexOf(" dupTableCellGray ") < 0
    }
  }

  // for director's view or completed view
  val ColorSelected = "baseButtonSelected"
  val ColorPlayed = "baseRequired"
  val ColorPartiallyPlayed = "baseRequiredNotNext"

  // for table view
  val ColorTablePlayed = "baseRequiredNotNext"

  val divPageBoardPrefix = """//div[contains(concat(' ', @class, ' '), ' pageBoard ')]"""
  val divViewBoardPrefix = """//div[contains(concat(' ', @class, ' '), ' viewBoard ')]"""

  case class Hand(
    north: String,
    south: String,
    east: String,
    west: String,
    contract: String,
    by: String,
    made: String,
    down: String,
    nsScore: String,
    ewScore: String,
    nsPoints: String,
    ewPoints: String,
    picture: String
  ) {
    def contains(p: String): Boolean = {
      p==north || p==south || p==east || p==west
    }
    def isPlayed: Boolean = contract != null && contract != ""
  }
}

trait PageWithBoardButtons {
  def clickBoardButton(
      board: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): HandPage
}

class BoardPage(
  val dupid: String,
  val viewtype: ViewType,
  val boardId: String
)(
  implicit
    val webDriver: WebDriver,
    pageCreated: SourcePosition
)
    extends Page[BoardPage]
    with PageWithBoardButtons
    with HandPicture[BoardPage] {
  import BoardPage._

  def getTableId(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Option[String] =
    eventually {
      val (dupid, viewtype, bid) = findIds
      viewtype match {
        case TableViewType(tid, rid) =>
          Some(tid)
        case _ =>
          None
      }
    }

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardPage =
    eventually {
      val buttons = findAllButtons
      buttons must contain key ("Game")
      getTableId match {
        case Some(tid) =>
          buttons("Table").text mustBe s"Table ${tid}"
        case _ =>
      }
      this
    }

  def checkBoardButtons(
      boards: Int*
  )(implicit patienceConfig: PatienceConfig, pos: Position): this.type =
    eventually {
      val buttons = findAllButtons
      boards.foreach(id => buttons must contain key (s"""Board_B${id}"""))
      this
    }

  def checkHandButtons(
      hands: Int*
  )(implicit patienceConfig: PatienceConfig, pos: Position): this.type =
    eventually {
      val buttons = findAllButtons
      hands.foreach(id => buttons must contain key (s"""Hand_T${id}"""))
      val allhandbuttons =
        buttons.keys.filter(k => k.startsWith("Hand_T")).toList
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
  def checkBoardButtons(current: Int, played: Boolean, boards: Int*)(implicit
      pos: Position
  ): this.type =
    eventually {
      val bs = boards.map(id => s"""Board_B${id}""")
      val buttonsOnPage = findBoardButtons(played).map { b =>
        (b, b.attribute("id").get)
      }
      buttonsOnPage.map(e => e._2) must contain allElementsOf (bs)
      val currentid = s"""Board_B${current}"""
      buttonsOnPage.foreach {
        case (el, id) =>
          if (id == currentid) {
            checkBoardButtonSelected(current)
          } else {
            if (played) {
              checkBoardButtonIdNoColor(id)
            } else {
              checkBoardButtonIdColor(ColorTablePlayed, id)
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
  def checkBoardButtonColors(
      current: Int,
      notplayed: List[Int],
      played: List[Int],
      partiallyplayed: List[Int]
  )(implicit pos: Position): this.type =
    eventually {
      checkBoardButtonSelected(current)
      checkBoardButtonNoColor(notplayed: _*)
      checkBoardButtonColor(
        ColorPartiallyPlayed,
        partiallyplayed.filter(id => id != current): _*
      )
      checkBoardButtonColor(ColorPlayed, played.filter(id => id != current): _*)
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
  private def checkBoardButtonColor(color: String, boards: Int*)(implicit
      pos: Position
  ): this.type =
    eventually {
      boards.foreach { board =>
        val bs = s"""Board_B${board}"""
        val elem = find(xpath(s"""//button[@id='${bs}']"""))
        withClue(s"""Button ${bs} has classes ${elem.attribute(
          "class"
        )}: expecting ${color}""") {
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
  private def checkBoardButtonIdColor(color: String, boards: String*)(implicit
      pos: Position
  ): this.type =
    eventually {
      boards.foreach { board =>
        val elem = find(xpath(s"""//button[@id='${board}']"""))
        withClue(s"""Button ${board} has classes ${elem.attribute(
          "class"
        )}: expecting ${color}""") {
          elem.containsClass(color) mustBe true
        }
      }
      this
    }

  /**
    * @param board
    * @throws TestFailedException
    */
  private def checkBoardButtonNoColor(
      boards: Int*
  )(implicit pos: Position): this.type =
    eventually {
      boards.foreach { board =>
        val bs = s"""Board_B${board}"""
        val elem = find(xpath(s"""//button[@id='${bs}']"""))
        (ColorSelected :: ColorPlayed :: ColorPartiallyPlayed :: Nil).foreach {
          color =>
            withClue(s"""Button ${bs} has classes ${elem.attribute(
              "class"
            )}: expecting to not find ${color}""") {
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
  private def checkBoardButtonIdNoColor(
      boards: String*
  )(implicit pos: Position): this.type =
    eventually {
      boards.foreach { board =>
        val elem = find(xpath(s"""//button[@id='${board}']"""))
        (ColorSelected :: ColorPlayed :: ColorPartiallyPlayed :: Nil).foreach {
          color =>
            withClue(s"""Button ${board} has classes ${elem.attribute(
              "class"
            )}: expecting to not find ${color}""") {
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
  def checkBoardButtonSelected(board: Int)(implicit pos: Position): this.type =
    checkBoardButtonColor(ColorSelected, board)

  def findBoardButtons(
      played: Boolean
  )(implicit pos: Position): List[Element] = {
    val p = if (played) "Played: " else "Unplayed: "
    val xp = s"""//span[b="${p}"]/button"""
    findElemsByXPath(xp)
  }

  def getHands(rows: List[List[Element]])(implicit pos: Position): List[Hand] = {
    rows.map { row =>
      Hand(
        row(0).text,
        row(1).text,
        row(2).text,
        row(3).text,
        row(4).text,
        row(5).text,
        row(6).text,
        row(7).text,
        row(8).text,
        row(9).text,
        row(10).text,
        row(11).text,
        row(12).text
      )
    }
  }

  def getHand(row: List[Element])(implicit pos: Position): Hand = {
    Hand(
      row(0).text,
      row(1).text,
      row(2).text,
      row(3).text,
      row(4).text,
      row(5).text,
      row(6).text,
      row(7).text,
      row(8).text,
      row(9).text,
      row(10).text,
      row(11).text,
      row(12).text
    )
  }

  /**
    * @param pos
    * @return all the rows.  Each element is a row, a list of the cells in a row
    */
  def getRows(implicit pos: Position): List[Hand] = {
    val rows = findElemsByXPath(
      divViewBoardPrefix + """/table/tbody/tr/td"""
    ).grouped(13).toList.map(l => getHand(l))
    log.fine(s"BoardPage.getRows: ${rows}")
    rows
  }

  /**
   * @param player a player that played the hand
   * @return the td elements in the row
   */
  private def getHandRow(player: Int)(implicit pos: Position): Option[Hand] = {
    val p = player.toString()
    val rows = getRows
    val r = rows.find { h =>
      h.contains(p)
    }
    log.fine(s"BoardPage.getHandRow(${player}): ${r}")
    r
  }

  def checkHandNotPlayed(n: Int, s: Int, e: Int, w: Int)(implicit pos: Position): BoardPage = {
    getHandRow(n) match {
      case Some(h) =>
        withClue(s"Hand results for players ${n},${s},${e},${w}: ${h}") {
          h.north mustBe n.toString()
          h.south mustBe s.toString()
          h.east mustBe e.toString()
          h.west mustBe w.toString()
          h.contract mustBe ""
          h.by mustBe ""
          h.made mustBe ""
          h.down mustBe ""
          h.nsScore mustBe ""
          h.ewScore mustBe ""
          h.nsPoints mustBe ""
          h.ewPoints mustBe ""
          h.picture mustBe ""
        }
        this
      case _ =>
        throw new Exception(s"Hand not found for players ${n},${s},${e},${w}")
    }
  }

  def checkHandPlayedWithCheckmarks(n: Int, s: Int, e: Int, w: Int)(implicit pos: Position): BoardPage = {
    getHandRow(n) match {
      case Some(h) =>
        withClue(s"Hand results for players ${n},${s},${e},${w}: ${h}") {
          h.north mustBe n.toString()
          h.south mustBe s.toString()
          h.east mustBe e.toString()
          h.west mustBe w.toString()
          h.contract mustBe Strings.checkmark
          h.by mustBe Strings.checkmark
          h.made mustBe Strings.checkmark
          h.down mustBe Strings.checkmark
          h.nsScore mustBe Strings.checkmark
          h.ewScore mustBe Strings.checkmark
          h.nsPoints mustBe Strings.checkmark
          h.ewPoints mustBe Strings.checkmark
          h.picture mustBe Strings.checkmark
        }
        this
      case _ =>
        throw new Exception(s"Hand not found for players ${n},${s},${e},${w}")
    }
  }

  def checkHandPlayed(
    n: Int,
    s: Int,
    e: Int,
    w: Int,
    contract: String,
    by: String,
    made: String,
    down: String,
    nsScore: String,
    ewScore: String,
    nsPoints: String,
    ewPoints: String,
    picture: String = ""
  )(implicit pos: Position): BoardPage = {
    getHandRow(n) match {
      case Some(h) =>
        withClue(s"Hand results for players ${n},${s},${e},${w}, nsPoint=${nsPoints}, ewPoint=${ewPoints}: ${h}") {
          h.north mustBe n.toString()
          h.south mustBe s.toString()
          h.east mustBe e.toString()
          h.west mustBe w.toString()
          h.contract mustBe contract
          h.by mustBe by
          h.made mustBe made
          h.down mustBe down
          h.nsScore mustBe nsScore
          h.ewScore mustBe ewScore
          h.nsPoints mustBe nsPoints
          h.ewPoints mustBe ewPoints
          h.picture mustBe picture
        }
        this
      case _ =>
        throw new Exception(s"Hand not found for players ${n},${s},${e},${w}")
    }
  }

  def checkHandPlayedContract(
      north: Int,
      south: Int,
      east: Int,
      west: Int,
      nsScore: Int,
      nsMP: Double,
      ewMP: Double,
      contractTricks: Int,
      contractSuit: ContractSuit,
      contractDoubled: ContractDoubled,
      declarer: PlayerPosition,
      madeOrDown: MadeOrDown,
      tricks: Int,
      vul: Vulnerability,
      validate: Boolean = true
  )(implicit pos: Position): BoardPage = {
    val contract = contractTricks match {
      case 0 => "Passed Out"
      case n =>
        s"""${contractTricks}${contractSuit.suit}${contractDoubled.forScore}${vul.forScore}"""
    }
    val (made, down) = madeOrDown match {
      case Made =>
        (s"${tricks}", "")
      case Down =>
        ("", s"${tricks}")
    }
    checkHandPlayed(
      north, south, east, west,
      contract,
      declarer.pos,
      made,
      down,
      s"${nsScore}",
      s"${-nsScore}",
      f"${nsMP}%1.0f",
      f"${ewMP}%1.0f",
      picture = ""
    )
  }

  def checkHandScores(eh: EnterHand) = {
            checkHandPlayedContract(
              eh.north,
              eh.south,
              eh.east,
              eh.west,
              eh.nsScore,
              eh.nsMP,
              eh.ewMP,
              eh.contractTricks,
              eh.contractSuit,
              eh.contractDoubled,
              eh.declarer,
              eh.madeOrDown,
              eh.tricks,
              eh.vul
            )

  }

  def checkPlayerNeverPlays(player: Int) = {
    getHandRow(player).isEmpty
  }

  def checkOthers(
      b: HandsOnBoard,
      allhands: AllHandsInMatch,
      checkmarks: Boolean = false
  )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage = {
    val allplayed =
      b.other.find(oh => oh.isInstanceOf[OtherHandNotPlayed]).isEmpty
    eventually {
      b.other.foreach { oh =>
        oh match {
          case OtherHandNotPlayed(tableid, roundid, boardid) =>
            val ob = allhands.getBoard(tableid, roundid, boardid)
            checkHandNotPlayed(ob.hand.north, ob.hand.south,ob.hand.east, ob.hand.west)
          case OtherHandPlayed(
                tableid,
                roundid,
                boardid,
                nsMP,
                ewMP,
                nsIMP,
                ewIMP
              ) =>
            val ob = allhands.getBoard(tableid, roundid, boardid)
            if (!allplayed && checkmarks) {
              checkHandPlayedWithCheckmarks(ob.hand.north, ob.hand.south,ob.hand.east, ob.hand.west)
            } else {
              val eh = ob.hand.copy(nsMP = nsMP, ewMP = ewMP)
              checkHandScores(eh)
            }
          case PlayerNotPlayingHand(boardid, player) =>
            checkPlayerNeverPlays(player)
        }
      }
    }
    this
  }

  def checkHand(
      round: Int,
      board: Int,
      allhands: AllHandsInMatch,
      checkmarks: Boolean
  )(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardPage = {
    withClue(
      s"""${pos.line} BoardPage.checkHand round ${round} board ${board} checkmarks ${checkmarks}"""
    ) {
      allhands.getBoardToRound(round, board) match {
        case Some(b) =>
          val allplayed =
            b.other.find(oh => oh.isInstanceOf[OtherHandNotPlayed]).isEmpty
          if (!allplayed && checkmarks) {
            checkHandPlayedWithCheckmarks(b.hand.north, b.hand.south, b.hand.east, b.hand.west)
          } else {
            checkHandScores(b.hand)
          }
          checkOthers(b, allhands, checkmarks)
        case None =>
          val maxRound = allhands.rounds.last
          allhands.getBoardToRound(maxRound, board) match {
            case Some(b) =>
              val allplayed =
                b.other.find(oh => oh.isInstanceOf[OtherHandNotPlayed]).isEmpty
              checkHandNotPlayed(b.hand.north, b.hand.south, b.hand.east, b.hand.west)
              def checkNotPlayed(table: Int, round: Int, brd: Int) = {
                val hob = allhands.getBoard(table, round, brd)
                checkHandNotPlayed(hob.hand.north, hob.hand.south, hob.hand.east, hob.hand.west)
              }
              b.other.foreach(oh =>
                oh match {
                  case OtherHandNotPlayed(table, round, board) =>
                    checkNotPlayed(table, round, board)
                  case OtherHandPlayed(
                        table,
                        round,
                        board,
                        nsMP,
                        ewMP,
                        nsIMP,
                        ewIMP
                      ) =>
                    checkNotPlayed(table, round, board)
                  case PlayerNotPlayingHand(board, player) =>
                    checkPlayerNeverPlays(player)
                }
              )
            case None =>

          }
      }
      this
    }
  }

  def clickHand(
      nplayer: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): HandPage =
    eventually {
      click on id(s"""Hand_${nplayer}""")

      new HandPage(dupid, viewtype, boardId, s"p${nplayer}")
    }

  def clickBoardButton(
      board: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): HandPage =
    clickUnplayedBoard(board)

  def clickPlayedBoard(
      board: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): BoardPage =
    eventually {
      click on id(s"""Board_B${board}""")
      findIds._3 mustBe s"""B${board}"""
      new BoardPage(dupid, viewtype, IndividualBoard.id(board).id)
    }

  def clickUnplayedBoard(
      board: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): HandPage =
    eventually {
//    click on id(s"""Board_B${board}""")
      val b = findElemById(s"""Board_B${board}""")
      b.scrollToElement
      b.enter
      new HandPage(dupid, viewtype, IndividualBoard.id(board).id, "")
    }

  def clickTableButton(
      table: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): TablePage = {
    getTableId match {
      case Some(id) =>
        if (id != table.toString())
          fail(s"""Not on board for table ${table}, on ${id}""")
        enterOnButton("Table")
        eventually { TablePage.current(Hands) }
      case None =>
        fail(s"""Not on board for table ${table}""")
    }
  }

  def clickScoreboard(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ScoreboardPage = {
    clickButton("Game")
    ScoreboardPage.waitFor
  }

}
