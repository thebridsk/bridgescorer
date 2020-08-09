package com.github.thebridsk.bridge.fullserver.test.pages.chicago

import com.github.thebridsk.browserpages.Page
import org.openqa.selenium.WebDriver
import com.github.thebridsk.source.SourcePosition
import com.github.thebridsk.utilities.logging.Logger
import org.scalatest.concurrent.Eventually._
import org.scalactic.source.Position
import com.github.thebridsk.browserpages.PageBrowser._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.browserpages.Element
import com.github.thebridsk.bridge.rotation.Table
import org.scalatest.Assertion
import scala.util.matching.Regex

object FiveSelectPartnersPage {

  val log: Logger = Logger[FiveSelectPartnersPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): FiveSelectPartnersPage = {
    val (chiid, roundid) = EnterNamesPage.findMatchRoundId
    new FiveSelectPartnersPage(chiid, roundid)
  }

  def urlFor(chiid: String, roundid: Int): String =
    EnterNamesPage.urlFor(chiid, roundid)
  def demoUrlFor(chiid: String, roundid: Int): String =
    EnterNamesPage.demoUrlFor(chiid, roundid)

  /**
    * @param chiid the chicago id
    * @param roundid the round ID, zero based.
    */
  def goto(chiid: String, roundid: Int)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): FiveSelectPartnersPage = {
    go to urlFor(chiid, roundid)
    new FiveSelectPartnersPage(chiid, roundid)
  }

  val buttonOK = "OK"
  val buttonCancel = "Cancel"
  val buttonReset = "Cancel"

  /**
    * get the ID of the player for sitting out
    *
    * @param player the players name
    * @return the id
    */
  private def toPlayerButtonId(player: String) = s"Player_$player"
  val playerIdPattern: Regex = "Player_(.+)".r

  val selectPairingPattern: Regex = "([^-]+)-(.+)".r
}

/**
  * @param chiid the chicago match id
  * @param roundid the round, 0 based
  * @param webDriver
  * @param pageCreated
  */
class FiveSelectPartnersPage(
    val chiid: String,
    val roundid: Int
)(implicit
    webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[FiveSelectPartnersPage] {
  import FiveSelectPartnersPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FiveSelectPartnersPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        roundid.toString() must not be "0" // only valid for the first round

        Some(currentUrl) must (contain
          .oneOf(urlFor(chiid, roundid), demoUrlFor(chiid, roundid)))

//    find( xpath( """//div[@id='BridgeApp']/div[1]/div[1]/div[3]/div[1]/h1""") ).text mustBe "Simple Rotation"

        val allButtons = buttonOK :: buttonCancel :: Nil

        findButtons(allButtons: _*)
        this
      }
    }

  def clickOK(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): HandPage = {
    clickButton(buttonOK)
    new HandPage(chiid, roundid, 0, ChicagoMatchTypeSimple)
  }

  def clickCancel(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SummaryPage = {
    clickButton(buttonCancel)
    SummaryPage.current(ChicagoMatchTypeSimple)
  }

  def clickReset(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FiveSelectPartnersPage = {
    clickButton(buttonReset)
    this
  }

  def isOKEnabled(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    getButton(buttonOK).isEnabled
  }

  /**
    * @param player the name
    * @return this
    */
  def clickPlayerSittingOut(player: String)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FiveSelectPartnersPage = {
    clickButton(toPlayerButtonId(player))
    this
  }

  def checkSittingOutPlayerNames(
      sittingOut: Option[String],
      notSittingOut: String*
  )(implicit patienceConfig: PatienceConfig, pos: Position): Unit = {
    val result =
      sittingOut.map(_ => true).toList ::: notSittingOut.map(_ => false).toList
    val players = sittingOut.toList ::: notSittingOut.toList

    withClue(
      s"""Checking for next sitting out player, sitting out $sittingOut, not sitting out $notSittingOut"""
    ) {
      val idplayers = players.map(toPlayerButtonId(_))
      val map = findButtons(idplayers: _*)

      idplayers.zip(result).foreach {
        case (player, res) =>
          withClue(s"""Checking player $player is${if (res) ""
          else " not"} sitting out: """) {
            map(player).containsClass("baseButtonSelected") mustBe res
          }
      }
    }

  }

  def getSittingOutPlayer(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Option[String] = {
    val possible = findAllSelectedButtons.toList.flatMap {
      case (id, e) =>
        id match {
          case playerIdPattern(player) => player :: Nil
          case _                       => Nil
        }
    }
    possible.length must be <= 1
    possible.headOption
  }

  def checkNotFoundPlayersForSittingOut(
      notfound: String*
  )(implicit patienceConfig: PatienceConfig, pos: Position): Assertion = {
    withClue(
      s"""Checking for next sitting out player, should not see $notfound"""
    ) {
      val allbuttons = findAllButtons.keySet
      allbuttons.intersect(notfound.map(toPlayerButtonId(_)).toSet) mustBe empty
    }
  }

  def getPairings(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): List[Pairings] = {
    val pairs = findAll(
      xpath(
        """//div[contains(concat(' ', @class, ' '), ' chiDivPageSelectPairs ')]/div[1]/div/p"""
      )
    )

    pairs.length % 2 mustBe 0

    def getPairs(
        list: List[Element],
        index: Int,
        result: List[Pairings]
    ): List[Pairings] = {
      list match {
        case Nil => result
        case one :: Nil =>
          fail("Did not get an even number of pairs")
        case one :: two :: rest =>
          val pair1 = one.text match {
            case selectPairingPattern(p1, p2) => (p1, p2)
            case s                            => fail(s"Error parsing pairing names: $s")
          }
          val pair2 = two.text match {
            case selectPairingPattern(p1, p2) => (p1, p2)
            case s                            => fail(s"Error parsing pairing names: $s")
          }
          val nextResult = Pairings(index, pair1, pair2) :: result
          val nextIndex = index + 1
          getPairs(rest, nextIndex, nextResult)
      }
    }

    getPairs(pairs, 0, Nil).reverse
  }

  /**
    * @param index 0 based
    * @param patienceConfig
    * @param pos
    */
  def clickPairing(index: Int)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FiveSelectPartnersPage = {
    clickButton(s"Fixture$index")
    this
  }

  def clickSwap(loc: PlayerPosition)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FiveSelectPartnersPage = {
    clickButton(s"Swap${loc.pos}")
    this
  }

  def clickClockwise(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FiveSelectPartnersPage = {
    clickButton("clockwise")
    this
  }

  def clickAntiClockwise(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FiveSelectPartnersPage = {
    clickButton("anticlockwise")
    this
  }

  def clickDealer(loc: PlayerPosition)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): FiveSelectPartnersPage = {
    clickButton(s"Dealer${loc.pos}")
    this
  }

  def checkDealer(dealer: Option[String], notDealer: String*)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Unit = {
    val result =
      dealer.map(_ => true).toList ::: notDealer.map(_ => false).toList
    val players = dealer.toList ::: notDealer.toList

    withClue(
      s"""Checking for next sitting out player, sitting out $dealer, not sitting out $notDealer"""
    ) {
      val idplayers = players.map(toPlayerButtonId(_))
      val map = findButtons(idplayers: _*)

      idplayers.zip(result).foreach {
        case (player, res) =>
          withClue(s"""Checking player $player is${if (res) ""
          else " not"} sitting out: """) {
            map(player).containsClass("baseButtonSelected") mustBe res
          }
      }
    }
  }

  def getSeats(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Table = {
    val players = findAll(
      xpath(
        """//div[contains(concat(' ', @class, ' '), ' chiDivPageSelectPos ')]/table/tbody/tr/td/b"""
      )
    ).map(_.text)
    Table(
      players(3),
      players(0),
      players(1),
      players(2),
      getSittingOutPlayer.getOrElse("")
    )
  }
}

/**
  * @param index 0 based
  * @param pair1
  * @param pair2
  */
case class Pairings(
    index: Int,
    pair1: (String, String),
    pair2: (String, String)
) {

  def compare(
      player1: String,
      player2: String,
      pair: (String, String)
  ): Boolean = {
    (player1 == pair._1 && player2 == pair._2) || (player1 == pair._2 && player2 == pair._1)
  }

  def containsPair(player1: String, player2: String): Boolean = {
    compare(player1, player2, pair1) || compare(player1, player2, pair2)
  }
}
