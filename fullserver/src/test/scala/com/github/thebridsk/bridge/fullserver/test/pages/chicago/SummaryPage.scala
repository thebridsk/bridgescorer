package com.github.thebridsk.bridge.fullserver.test.pages.chicago

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.bridge.data.Id
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.browserpages.Element
import com.github.thebridsk.bridge.fullserver.test.pages.BaseHandPage
import com.github.thebridsk.browserpages.PagesAssertions._
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.Popup
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage

object SummaryPage {

  val log = Logger[SummaryPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (cid, round) = findChicagoIds
    new SummaryPage( cid, round )
  }

  def waitFor(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (cid, round) = eventually { findChicagoIds }
    new SummaryPage( cid, round )
  }

  def goto( id: String, round: Option[Int] = None )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to getUrl(id, round)
    new SummaryPage( id, round )
  }

  def demo( id: String, round: Option[Int] = None )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to getDemoUrl(id, round)
    new SummaryPage( id, round )
  }

  def getUrl( id: String, round: Option[Int] = None ) = {
    val r = round.map( rid=> s"/rounds/${rid}" ).getOrElse("")
    TestServer.getAppPageUrl(s"chicago/${id}${r}")
  }

  def getDemoUrl( id: String, round: Option[Int] = None ) = {
    val r = round.map( rid=> s"/rounds/${rid}" ).getOrElse("")
    TestServer.getAppDemoPageUrl(s"chicago/${id}${r}")
  }

  val buttonQuit = "Quit"
  val buttonNewRound = "NewRound"
  val buttonNextHand = "NextHand"
  val button6HandRound = "6HandRound"
  val button8HandRound = "8HandRound"
  val buttonAllRounds = "Summary"
  val buttonInputStyle = "InputStyle"

  def roundToButtonId( r: Int ) = s"Round${r+1}"
  def handToButtonId( h: Int ) = s"Hand_${h+1}"

  val buttonIdsAlways = List( buttonQuit, buttonInputStyle )

  private val patternComplete = """(C\d+)(?:/rounds/(\d+))?""".r

  /**
   * Get the chicago ID, the round id.
   * currentUrl needs to be one of the following:
   *   chicago/C10
   *   chicago/C10/rounds/0
   * @return Tuple2(
   *            chiid
   *            Option(roundid)  - roundid is an Int, zero based
   *          )
   */
  def findChicagoIds(implicit webDriver: WebDriver, pos: Position): (String, Option[Int]) = {
    val prefix = TestServer.getAppPageUrl("chicago/")
    val prefix2 = TestServer.getAppDemoPageUrl("chicago/")
    val cur = currentUrl
    withClueEx(s"Unable to determine chicago id in SummaryPage: ${cur}") {
      val rest = if (cur.startsWith(prefix)) {
        cur.drop(prefix.length())
      } else if (cur.startsWith(prefix2)) {
        cur.drop(prefix2.length())
      } else {
        fail(s"URL does not start with $prefix2 or $prefix: $cur")
      }
      rest match {
        case patternComplete(cid,rid) => (cid, Option(rid).map(r=>r.toInt))
        case _ => fail(s"URL did not match pattern ${patternComplete}")
      }
    }
  }

  case class TotalsTable( players: List[String], rounds: List[List[String]], totals: List[String] ) {

    def playerIndex( p: String ) = {
      val i = players.indexOf(p)
      if (i<0) fail(s"Unable to find ${p} in table")
      i
    }

    def getRound( r: Int ) = rounds(r)

    /**
     * @param p the player
     * @param r the round id, 0 based
     */
    def getScore( p: String, r: Int ) = {
      withClueEx(s"Looking for player ${p} in round ${r} in TotalsTable.getScore") {
        getRound(r)( playerIndex(p) )
      }
    }

    def getTotal( p: String ) = {
      withClueEx(s"Looking for player ${p} in TotalsTable.getTotal") {
        totals( playerIndex(p) )
      }
    }

    def checkNumberRounds( n: Int ) = {
      withClueEx(s"Must have ${n} rounds in TotalsTable") {
        rounds.length mustBe n
      }
    }

    /**
     * @param p the player
     * @param tot the total
     * @return this
     */
    def checkTotal( p: String, tot: String ) = {
      withClueEx(s"Player ${p} total ${tot} in TotalsTable") {
        getTotal(p) mustBe tot
        this
      }
    }

    /**
     * @param p the player
     * @param r the round id, 0 based
     * @param score
     * @return this
     */
    def checkScore( p: String, r: Int, score: String ) = {
      withClueEx(s"Player ${p} in round ${r} score ${score} in TotalsTable") {
        getScore(p,r) mustBe score
        this
      }
    }
  }

  case class RoundTableRow( hand: String, contract: String, by: String, made: String, tricks: String, Dealer: String, scores: List[String] ) {

    def checkTotal( team: Int, score: String ) = {
      withClueEx(s"Team ${team} checking total ${score} in RoundTableRow") {
        scores(team) mustBe score
        this
      }
    }

  }

  val patternRoundInRoundTable = """Round (\d+)""".r

  /**
   * @param round the round, 1 based
   */
  case class RoundTable( round: Int,
                         teams: List[(String,String)],
                         rows: List[RoundTableRow],
                         total: List[String] ) {

    def getTeam( p: String )(implicit pos: Position) = {
      teams.zipWithIndex.find{ e =>
        val ((p1,p2),i) = e
        p1==p || p2==p
      }.map{ _._2 }.getOrElse(fail( s"Did not find player ${p} from ${pos.line}" ))
    }

    def checkTotal( p: String, tot: String )(implicit pos: Position) = {
      withClueEx(s"Player ${p} total ${tot} in RoundTable") {
        total( getTeam(p) ) mustBe tot
        this
      }
    }

    /**
     * @param p
     * @param roundid the round, zero based
     * @param score
     */
    def checkScore( p: String, hand: Int, score: String ) = {
      withClueEx(s"Player ${p} in hand ${hand} score ${score} in RoundTable") {
        rows(hand).checkTotal(getTeam(p), score)
        this
      }
    }
  }

}

class SummaryPage(
                      val chiid: String,
                      val round: Option[Int] = None
                    )( implicit
                        webDriver: WebDriver,
                        pageCreated: SourcePosition
                    ) extends Page[SummaryPage] {

  import SummaryPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    eventually {
      val (cid,roundid) = findChicagoIds

      cid mustBe chiid
      roundid mustBe round
    }

    val buttons = eventually{ findButtons( buttonIdsAlways: _* ) }

    this
  }}

  def clickQuit(implicit pos: Position) = {
    withClueEx(s"${pos.line}: trying to click quit") {
      clickButton(buttonQuit)
      GenericPage.current
    }
  }

  def clickInputStyle(implicit pos: Position) = {
    withClueEx(s"${pos.line}: trying to click input style") {
      clickButton(buttonInputStyle)
      this
    }
  }

  def getInputStyle(implicit patienceConfig: PatienceConfig, pos: Position): Option[String] =
    BaseHandPage.getInputStyle

  /**
   * @param style the input style, valid values are Guide, Prompt, Original
   * @return Some(style) if successful, otherwise returns current input style
   */
  def setInputStyle( style: String ): Option[String] =
    BaseHandPage.setInputStyle(style)

  /**
   * @param r the round, 0 based
   */
  def clickRound( r: Int )(implicit pos: Position) = {
    withClueEx(s"${pos.line}: trying to click round ${r}") {
      clickButton( roundToButtonId(r) )
      new SummaryPage( chiid, Some(r) )
    }
  }

  /**
   * Click a hand button.  There must only be one round table on page.
   * @param h the hand, 0 based
   */
  def clickHand( h: Int )(implicit pos: Position) = {
    withClueEx(s"${pos.line}: trying to click hand ${h}") {
      val e = findAll(id(handToButtonId(h)))
      e.length mustBe 1
      e(0).click
      HandPage.waitFor
    }
  }

  /**
   * Click a hand button in the specified round
   * @param h the hand, 0 based
   * @param r the round, 0 based
   */
  def clickHand( h: Int, r: Int )(implicit pos: Position) = {
    withClueEx(s"${pos.line}: trying to click hand ${h} in round ${r}") {
      val e = findAll(id(handToButtonId(h)))
      e.length must be > r
      e(r).click
      HandPage.waitFor
    }
  }

  def clickNextHand(implicit pos: Position) = {
    withClueEx(s"${pos.line}: trying to click next hand") {
      clickButton(buttonNextHand)
      HandPage.waitFor
    }
  }

  def clickNewRound(implicit pos: Position) = {
    withClueEx(s"${pos.line}: trying to click new round") {
      clickButton(buttonNewRound)
      GenericPage.current
    }
  }

  def click6HandRound(implicit pos: Position) = {
    withClueEx(s"${pos.line}: trying to click 6 hand round") {
      clickButton(button6HandRound)
      HandPage.waitFor
    }
  }

  def click8HandRound(implicit pos: Position) = {
    withClueEx(s"${pos.line}: trying to click 6 hand round") {
      clickButton(button8HandRound)
      HandPage.waitFor
    }
  }

  def clickAllRounds(implicit pos: Position) = {
    withClueEx(s"${pos.line}: trying to click all rounds") {
      clickButton(buttonAllRounds)
      new SummaryPage(chiid)
    }
  }

  def getAllButtons(implicit pos: Position) = {
    findAllButtons
  }

  def checkButtons(visible: List[String], notvisible: List[String] = Nil)(implicit pos: Position) = {
    val buttons = getAllButtons.keys.toList
    if (visible.find { v => !buttons.contains(v) }.isDefined) {
      fail( s"Did not find all buttons (${visible}) in ${buttons}" )
    }
    if (notvisible.find { v => buttons.contains(v) }.isDefined) {
      fail( s"Found a button from (${notvisible}) in ${buttons}" )
    }
    this
  }

  def is6HandRoundButtonVisible(implicit pos: Position) = {
    getAllButtons.keys.find(k => k==button6HandRound).isDefined
  }

  def is8HandRoundButtonVisible(implicit pos: Position) = {
    getAllButtons.keys.find(k => k==button8HandRound).isDefined
  }

  /**
   * @return a TotalsTable object
   */
  def getTotalsTable(implicit pos: Position) = {
    withClueEx("getTotalsTable") {
      val names = getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' chiChicagoSummaryPage ')]/div[1]/table/thead/tr[2]/th""").drop(1).map(e => e.text)
      val rows = getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' chiChicagoSummaryPage ')]/div[1]/table/tbody/tr""")
      val scores = rows.map { r =>
        r.findAll(xpath("""./td""")).drop(1).map(e=>e.text)
      }
      val totals = getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' chiChicagoSummaryPage ')]/div[1]/table/tfoot/tr/td""").drop(1).map(e=>e.text)

      TotalsTable(names,scores,totals)
    }
  }

  private def getRoundTableRow( row: Element )(implicit pos: Position): RoundTableRow = {
    val cells = row.findAll(xpath("./td")).map{ e => e.text }
    val hand = cells(0)
    val contract = cells(1)
    val by = cells(2)
    val made = cells(3)
    val tricks = cells(4)
    val Dealer = cells(5)
    val score1 = cells(6)
    val score2 = cells(7)

    RoundTableRow( hand, contract, by, made, tricks, Dealer, List(score1, score2) )
  }

  private def getNamesFromHeader( th: Element )(implicit pos: Position): (String, String) = {
    withClueEx("getNamesFromHeader") {
//      val t = th.findAll(xpath("./text()")).map {e => e.text}
      val t = getAllTextNodes(th)
      t.length mustBe 2
      ( t(0), t(1) )
    }
  }

  private def getRoundTableFromElement( div: Element )(implicit pos: Position): RoundTable = {
    withClueEx("getRoundTableFromElement") {
      val header = div.findAll(xpath("./table/thead/tr[1]/th"))
      header.length mustBe 3
      val roundString = header(0).text
      val round: Int = roundString match {
        case patternRoundInRoundTable(r) => r.toInt
        case _ => fail(s"Unable to determine round from ${roundString}")
      }
      val team1 = getNamesFromHeader(header(1))
      val team2 = getNamesFromHeader(header(2))
      val rawrounds = div.findAll(xpath("./table/tbody/tr"))
      val rounds = rawrounds.map(r => getRoundTableRow(r))
      val rawtotal = div.findAll(xpath("./table/tfoot/tr/td")).drop(1).map(e=>e.text)
      val total1 = rawtotal(0)
      val total2 = rawtotal(1)
      RoundTable(round,List(team1,team2),rounds,List(total1,total2))
    }
  }

  private def dropFirstAndLast[T]( list: List[T] ): List[T] = {
    val x = list.drop(1)
    x.take(x.length-2)
  }

  def getRoundTables(implicit pos: Position): List[RoundTable] = {
    withClueEx("getRoundTables") {
      val alldivs = getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' chiChicagoSummaryPage ')]/div""")
      val divs = dropFirstAndLast(alldivs).map(d => getRoundTableFromElement(d))
      divs
    }
  }

  /**
   * Get a RoundTable from the page.  There must only be one round table
   */
  def getOnlyRoundTable()(implicit pos: Position): RoundTable = {
    withClueEx("Looking for only one RoundTable") {
      val rawdivs = getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' chiChicagoSummaryPage ')]/div""")
      rawdivs.length mustBe 4
      val alldivs = dropFirstAndLast(rawdivs)
      alldivs.length mustBe 1
      getRoundTableFromElement(alldivs(0))
    }
  }

  /**
   * Get a RoundTable from the page.
   * @param index the index, 0 based.
   */
  def getRoundTable( index: Int )(implicit pos: Position): RoundTable = {
    withClueEx("getRoundTable ${index}") {
      val alldivs = dropFirstAndLast(getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' chiChicagoSummaryPage ')]/div"""))
      alldivs.length must be > index
      getRoundTableFromElement(alldivs(index))
    }
  }

  /**
   * Check the hand scores when only one round table is displayed
   * @param players the players to check
   * @param scores the scores, index matches players
   */
  def checkHandScore( hand: Int, players: List[String], scores: List[String], totals: List[String] )(implicit pos: Position) = {
    val rt = getOnlyRoundTable()
    try {
      withClueEx( s"Checking hand ${hand} with players ${players} scores ${scores}" ) {
        players.zip(scores).foreach { e =>
          val (p,s) = e
          rt.checkScore(p, hand, s)
        }
      }
      withClueEx( s"Checking totals with players ${players} totals ${totals}" ) {
        players.zip(totals).foreach { e =>
          val (p,s) = e
          rt.checkTotal(p, s)
        }
        this
      }
    } catch {
      case x: Exception =>
        log.severe(s"""checkHandScore failure.\n${rt}\n""",x)
        throw x
    }
  }

  /**
   * Check the hand scores when only one round table is displayed
   * @param players the players to check
   * @param scores the scores, index matches players
   */
  def checkTotalScore( round: Int, players: List[String], scores: List[String], totals: List[String] )(implicit pos: Position) = {
    val rt = getTotalsTable
    try {
      withClueEx( s"Checking summary for round ${round} with players ${players} scores ${scores}" ) {
        players.zip(scores).foreach { e =>
          val (p,s) = e
          rt.checkScore(p, round, s)
        }
      }
      withClueEx( s"Checking summary totals with players ${players} totals ${totals}" ) {
        players.zip(totals).foreach { e =>
          val (p,s) = e
          rt.checkTotal(p, s)
        }
        this
      }
    } catch {
      case x: Exception =>
        log.severe(s"""checkTotalScore failure.\n${rt}\n""",x)
        throw x
    }
  }

  /**
   * @param pos
   */
  def clickHome( implicit pos: Position ) = {
    clickButton("Home")
    new HomePage()(webDriver,pos)
  }

}
