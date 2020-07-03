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
import com.github.thebridsk.browserpages.GenericPage

object FairSelectPartnersPage {

  val log = Logger[FairSelectPartnersPage]()

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (chiid,roundid) = EnterNamesPage.findMatchRoundId
    new FairSelectPartnersPage(chiid,roundid)
  }

  def urlFor( chiid: String, roundid: Int ) = EnterNamesPage.urlFor(chiid,roundid)
  def demoUrlFor( chiid: String, roundid: Int ) = EnterNamesPage.demoUrlFor(chiid,roundid)

  /**
   * @param chiid the chicago id
   * @param roundid the round ID, zero based.
   */
  def goto( chiid: String, roundid: Int )( implicit
              webDriver: WebDriver,
              patienceConfig: PatienceConfig,
              pos: Position
          ) = {
    go to urlFor(chiid,roundid)
    new FairSelectPartnersPage(chiid,roundid)
  }

  val buttonOK = "OK"
  val buttonReset = "Reset"
  val buttonCancel = "Cancel"

  /**
    * get the ID of the player for sitting out
    *
    * @param player the players name
    * @return the id
    */
  private def toPlayerButtonId( player: String ) = s"Player_$player"
}

/**
  *
  *
  * @param chiid the chicago match id
  * @param roundid the round, 0 based
  * @param webDriver
  * @param pageCreated
  */
class FairSelectPartnersPage(
    val chiid: String,
    val roundid: Int
)( implicit
      webDriver: WebDriver,
      pageCreated: SourcePosition
) extends Page[FairSelectPartnersPage] {
  import FairSelectPartnersPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    roundid.toString() must not be "0"      // only valid for the first round

    Some(currentUrl) must contain oneOf( urlFor(chiid,roundid), demoUrlFor(chiid,roundid) )

    find( xpath( """//div[@id='BridgeApp']/div[1]/div[1]/div[3]/div[1]/h1""") ).text mustBe "Fair Rotation"

    val allButtons = buttonOK::buttonReset::buttonCancel::Nil

    findButtons( allButtons: _* )
    this
  }}

  def clickOK(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonOK)
    new HandPage(chiid,roundid,0,ChicagoMatchTypeFair)
  }

  def clickReset(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonReset)
    this
  }

  def clickCancel(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(buttonCancel)
    SummaryPage.current(ChicagoMatchTypeFair)
  }

  def isOKEnabled(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getButton(buttonOK).isEnabled
  }

  def isResetEnabled(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getButton(buttonReset).isEnabled
  }

  /**
    * Click the player at the position, to set that player there.
    *
    * @param loc the location of the player.
    *             East is the player to the left of dealer
    *             South is the partner of dealer
    *             West is the player to the right of dealer
    * @param i the player, values are: 1, 2, 3  Not all options are available.
    * @param patienceConfig
    * @param pos
    * @return
    */
  def clickSittingOutPlayer( player: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton( toPlayerButtonId(player) )
    this
  }

  def checkSittingOutPlayerNames( sittingOut: Option[String], notSittingOut: String* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val result = sittingOut.map( _ => true ).toList:::notSittingOut.map( _ => false ).toList
    val players = sittingOut.toList:::notSittingOut.toList

    withClue(s"""Checking for next sitting out player, sitting out $sittingOut, not sitting out $notSittingOut""") {
      val idplayers = players.map( toPlayerButtonId(_))
      val map = findButtons(idplayers: _*)

      idplayers.zip(result).foreach { case (player, res) =>
        withClue(s"""Checking player $player is${if (res) "" else " not"} sitting out: """) {
          map(player).containsClass("baseButtonSelected") mustBe res
        }
      }
    }

  }

  def checkNotFoundPlayersForSittingOut( notfound: String* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    withClue(s"""Checking for next sitting out player, should not see $notfound""") {
      val allbuttons = findAllButtons.keySet
      allbuttons.intersect( notfound.map(toPlayerButtonId(_)).toSet) mustBe empty
    }
  }


  /**
    * Check the player positions
    *
    * @param table which table to check, valid values: "Prior hand" and "Next hand"
    * @param dealer
    * @param north
    * @param south
    * @param east
    * @param west
    * @param extra
    *
    */
  def checkPositions( table: String, dealer: PlayerPosition,
                      north: String, south: String, east: String, west: String, extra: String ) = {

    //  <div>
    //    <h1>Prior hand</h1>    or Next hand
    //    <table>

    // format of table:
    //   row  cells
    //    1   sittingOut
    //    2   empty  south  empty
    //    3   east   west
    //    4   empty  north  empty

    //  //div/h1[text()="Last hand"]/following-sibling::table

    val current = findAll(xpath(s"""//div/p[text()="${table}"]/following-sibling::table/tbody/tr/td""")).toList

//    current.foreach(e => println(s"checkPosition $table ${e.text}") )

    current.size mustBe 9

    def getText( pos: PlayerPosition, player: String ) = if (pos == dealer) s"Dealer\n$player" else player

    current(0).text mustBe s"Sitting out\n$extra"
    current(2).text mustBe getText(South,south)
    current(4).text mustBe getText(East,east)
    current(5).text mustBe getText(West,west)
    current(7).text mustBe getText(North,north)

  }

}
