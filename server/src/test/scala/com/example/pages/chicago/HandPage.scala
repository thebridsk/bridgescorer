package com.example.pages.chicago

import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.pages.PageBrowser._
import com.example.test.selenium.TestServer
import utils.logging.Logger
import com.example.pages.Page
import com.example.pages.BaseHandPage
import com.example.data.bridge._
import com.example.data.Board
import com.example.data.util.Strings
import com.example.pages.duplicate.ScoreboardPage.PlaceEntry
import com.example.data.BoardSet
import com.example.data.Movement
import com.example.pages.GenericPage

object HandPage {

  val log = Logger[HandPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (chiid,roundid,hand) = findIds
    new HandPage(chiid,roundid,hand)
  }

  def waitFor(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (chiid,roundid,hand) = eventually { findIds }
    new HandPage(chiid,roundid,hand)
  }

  /**
   * @param chiid
   * @param round the round, zero based
   * @param hand the hand in the round, zero based
   * @return a HandPage object
   */
  def goto(chiid: String, round: Int, hand: Int )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor(chiid,round,hand)
    new HandPage(chiid,round,hand)
  }

  /**
   * @param chiid
   * @param round the round, zero based
   * @param hand the hand in the round, zero based
   * @return the URL
   */
  def urlFor(chiid: String, round: Int, hand: Int) = {
    // http://loopback:8080/public/index-fastopt.html#chicago/C10/rounds/0/hands/0
    TestServer.getAppPageUrl(s"chicago/${chiid}/rounds/${round}/hands/${hand}")
  }

  // http://loopback:8080/public/index-fastopt.html#chicago/C10/rounds/0/hands/0
  val patternForIds = """(C\d+)/rounds/(\d+)/hands/(\d+)""".r

  /**
   * @return Tuple with 3 elements.  (chiid,round,hand)
   *       chiid (String) the Chicago ID
   *       round (Int) the round, zero based
   *       hand  (Int) the hand in the round, zero based
   */
  def findIds(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val prefix = TestServer.getAppPageUrl("chicago/")
    val cur = currentUrl
    withClue(s"Unable to determine duplicate id in HandPage: ${cur}") {
      cur must startWith (prefix)
      cur.drop( prefix.length() ) match {
        case patternForIds(cid,roundid,handid) => (cid,roundid.toInt,handid.toInt)
        case _ => fail(s"URL did not match pattern ${patternForIds}")
      }
    }
  }
}

class HandPage(chiid: String, round: Int, hand: Int )( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends BaseHandPage {
  import HandPage._

  override
  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
    eventually {
      findIds mustBe (chiid,round,hand)
    }
    super.validate
    this
  }

  override
  def clickOk(implicit patienceConfig: PatienceConfig, pos: Position) = {
    super.clickOk
    SummaryPage.current
  }

  /**
   * Enter the contract.
   * @param contractTricks
   * @param contractSuit
   * @param contractDoubled
   * @param declarer
   * @param madeOrDown
   * @param tricks
   * @param patienceConfig
   * @param pos
   * @return this
   */
  def enterContract(
        contractTricks: Int,
        contractSuit: ContractSuit,
        contractDoubled: ContractDoubled,
        declarer: PlayerPosition,
        madeOrDown: MadeOrDown,
        tricks: Int,
      )(implicit
          patienceConfig: PatienceConfig,
          pos: Position
      ): this.type = {
    enterContract(contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks, None, None)
  }

  /**
   * Validate that all the selected buttons match the arguments.
   * If any of the arguments is None, then none of those buttons must be selected.
   * @param contractTricks
   * @param contractSuit
   * @param contractDoubled
   * @param declarer
   * @param madeOrDown
   * @param tricks
   * @param patienceConfig
   * @param pos
   */
  def validateContract(
        contractTricks: Option[Int],
        contractSuit: Option[ContractSuit],
        contractDoubled: Option[ContractDoubled],
        declarer: Option[PlayerPosition],
        madeOrDown: Option[MadeOrDown],
        tricks: Option[Int]
      )(implicit
          patienceConfig: PatienceConfig,
          pos: Position
      ): this.type = {
    validateContract(contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks, false, None, None)
    this
  }

  /**
   * Enter the contract and click OK.
   * @param contractTricks
   * @param contractSuit
   * @param contractDoubled
   * @param declarer
   * @param madeOrDown
   * @param tricks
   * @param score check the score line.  Don't check if None
   * @param dealer check for dealer.  Don't check if None
   * @param patienceConfig
   * @param pos
   * @return the next page
   */
  def enterHand(
        contractTricks: Int,
        contractSuit: ContractSuit,
        contractDoubled: ContractDoubled,
        declarer: PlayerPosition,
        madeOrDown: MadeOrDown,
        tricks: Int,
        nsVul: Vulnerability,
        ewVul: Vulnerability,
        score: Option[String] = None,
        dealer: Option[String] = None
      )(implicit
          patienceConfig: PatienceConfig,
          pos: Position
      ) = {
    dealer.foreach { d => checkDealer(d) }
    checkVulnerable(North, nsVul)
    checkVulnerable(East, ewVul)
    enterContract(contractTricks, contractSuit, contractDoubled, declarer, madeOrDown, tricks, None, None)
    score.foreach { s =>
      val sc = getScore
      sc._1 mustBe s"Score: ${s}"
    }
    clickOk
  }

}
