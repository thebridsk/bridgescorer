package com.github.thebridsk.bridge.server.test.pages.duplicate

import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually.PatienceConfig
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.data.BoardSet
import com.github.thebridsk.bridge.data.Movement
import java.net.URL
import com.github.thebridsk.bridge.server.test.pages.duplicate.ScoreboardPage.CompletedViewType
import com.github.thebridsk.bridge.server.test.pages.duplicate.ScoreboardPage.TableViewType
import com.github.thebridsk.bridge.data.bridge.PlayerPosition
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.browserpages.Page.AnyPage
import com.github.thebridsk.bridge.data.bridge._
import com.github.thebridsk.browserpages.TextField
import com.github.thebridsk.browserpages.PageBrowser
import org.openqa.selenium.Keys

object TableEnterOrSelectNamesPage {

  val log = Logger[TableEnterOrSelectNamesPage]

  def current( targetBoard: Option[String])(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val (dupid,tableid,roundid,targetboard) = findTableRoundId
    new TableEnterOrSelectNamesPage(dupid,tableid,roundid,targetBoard)
  }

  def urlFor( dupid: String, tableid: String, roundid: String, board: Option[String] ) =
    TableEnterScorekeeperPage.urlFor(dupid, tableid, roundid, board)

  def goto( dupid: String,
            tableid: String,
            roundid: String,
            board: Option[String] = None
          )( implicit
              webDriver: WebDriver,
              patienceConfig: PatienceConfig,
              pos: Position
          ) =
      TableEnterScorekeeperPage.goto(dupid, tableid, roundid, board)

  /**
   * Get the table id
   * currentUrl needs to be one of the following:
   *   duplicate/dupid/table/tableid/round/roundid/teams
   * @return (dupid, tableid,roundid)
   */
  def findTableRoundId(implicit webDriver: WebDriver, pos: Position) =
    TableEnterScorekeeperPage.findTableRoundId

  private def toInputName( loc: PlayerPosition ) = s"I_${loc.pos}"

  val buttonOK = "OK"
  val buttonReset = "Reset"
  val buttonCancel = "Cancel"
}

class TableEnterOrSelectNamesPage( dupid: String,
                                 tableid: String,
                                 roundid: String,
                                 targetBoard: Option[String]
                               )( implicit
                                   webDriver: WebDriver,
                                   pageCreated: SourcePosition
                               ) extends Page[TableEnterOrSelectNamesPage] {
  import TableEnterOrSelectNamesPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor(dupid,tableid,roundid,targetBoard)

    findButtons( buttonOK, buttonReset, buttonCancel )
    this
  }}

  val x = getButton("OK")

  def playEnterNames(north: String, south: String, east: String, west: String,
                     scorekeeper: PlayerPosition
                    )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): AnyPage = {

    getButton("OK")

    val inputs = findAllTextInputs
    if (inputs.contains("Scorekeeper")) {
      enterNamesAndClickOk(north, south, east, west,scorekeeper)
    } else if (inputs.contains("I_N")) {
      inputs("I_N").value = north
      inputs("I_S").value = south
      esc
      clickButton("OK")
      selectNamesAndClickButton(north, south, east, west, "OK", scorekeeper)
    } else if (inputs.contains("I_E")) {
      inputs("I_E").value = east
      inputs("I_W").value = west
      esc
      clickButton("OK")
      selectNamesAndClickButton(north, south, east, west, "OK", scorekeeper)
    } else {
      selectNamesAndClickButton(north, south, east, west, "OK", scorekeeper)
    }

    targetBoard match {
      case Some(boardid) => HandPage.current
      case None => new ScoreboardPage(Some(dupid), TableViewType(tableid,roundid) )
    }

  }

  /**
   * Enter the player names and click the specified button.
   * Must be on page #duplicate/M1/table/1/round/3/teams
   */
  def enterNamesAndClickButton( nsPlayer1: String, nsPlayer2: String,
                                ewPlayer1: String, ewPlayer2: String,
                                button: String,
                                scorekeeper: PlayerPosition
                              )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {

    val (scorekeeperName, partner) = scorekeeper match {
      case North => (nsPlayer1,South)
      case South => (nsPlayer2,North)
      case East => (ewPlayer1,West)
      case West => (ewPlayer2,East)
    }

    val inputScorekeeper = eventually {
      getCombobox("Scorekeeper")
    }
    inputScorekeeper.value = scorekeeperName

    esc

    clickButton( s"SK_${scorekeeper.pos}" )

    clickButton("OK")

    val inputs1 = eventually( {
       val map = findAllInputs(None)
       val List(a,b,c) = (North::South::East::West::Nil).filter(p=>p!=scorekeeper).map(p=>s"I_${p.pos}").toList
       map.keySet must contain allOf (a,b,c)
       map
    } )

    getElemById(s"Player_${scorekeeper.pos}").text mustBe scorekeeperName

    inputs1.foreach { entry => {
      val (name, input) = entry
      val t = new TextField(input)
      name match {
        case "I_N" => t.value=nsPlayer1
        case "I_S" => t.value=nsPlayer2
        case "I_E" => t.value=ewPlayer1
        case "I_W" => t.value=ewPlayer2
        case x => log.fine( "Found an input field with name "+x )
      }
      esc
    }}

    testNames(nsPlayer1, nsPlayer2, ewPlayer1, ewPlayer2,scorekeeper)

    eventually {
      val b = findButton(button)
      b.isEnabled mustBe true
      PageBrowser.scrollToElement(b.underlying)
//      Thread.sleep(50L)
//      b.click
      val k = Keys.ENTER
      b.sendKeys(k.toString())
    }
  }

  /**
   * Select the player names and click the specified button.
   * Must be on page #duplicate/M1/table/1/round/3/teams
   */
  def selectNamesAndClickButton( nsPlayer1: String, nsPlayer2: String,
                                 ewPlayer1: String, ewPlayer2: String,
                                 button: String,
                                 scorekeeper: PlayerPosition
                               )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {

    val (scorekeeperName, partner) = scorekeeper match {
      case North => (nsPlayer1,South)
      case South => (nsPlayer2,North)
      case East => (ewPlayer1,West)
      case West => (ewPlayer2,East)
    }

    clickButton(s"P_$scorekeeperName")

    clickButton(s"SK_${scorekeeper.pos}")

    clickButton( "OK" )

    val inputs1 = List("Player_N", "Player_S", "Player_E", "Player_W").map( p => {
      val e = findElemById(p)
      ( p, e)
    }).toMap

    var swap = false

    inputs1.foreach { entry => {
      val (name, input) = entry
      val t = input.text
      name match {
        case "Player_N" => swap = swap || (North!=scorekeeper&&North!=partner&&t != nsPlayer1)
        case "Player_S" => swap = swap || (South!=scorekeeper&&South!=partner&&t != nsPlayer2)
        case "Player_E" => swap = swap || (East!=scorekeeper&&East!=partner&&t != ewPlayer1)
        case "Player_W" => swap = swap || (West!=scorekeeper&&West!=partner&&t != ewPlayer2)
        case x => log.fine( "Found an input field with name "+x )
      }
    }}

    if (swap) {
      clickButton("Swap_left")
    }

    testNamesNoInput(nsPlayer1, nsPlayer2, ewPlayer1, ewPlayer2)

    clickButton(button)
  }

  /**
   * Enter the player names.
   * Must be on page #duplicate/M1/table/1/round/3/teams
   */
  def enterNamesAndClickOk( nsPlayer1: String, nsPlayer2: String,
                            ewPlayer1: String, ewPlayer2: String,
                            scorekeeper: PlayerPosition
                          )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {

    enterNamesAndClickButton(nsPlayer1, nsPlayer2, ewPlayer1, ewPlayer2, "OK", scorekeeper)
  }

  def testNames( nsPlayer1: String, nsPlayer2: String,
                 ewPlayer1: String, ewPlayer2: String,
                 scorekeeper: PlayerPosition
               )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {

    val (scorekeeperName, partner) = scorekeeper match {
      case North => (nsPlayer1,South)
      case South => (nsPlayer2,North)
      case East => (ewPlayer1,West)
      case West => (ewPlayer2,East)
    }

    getElemById(s"Player_${scorekeeper.pos}").text mustBe scorekeeperName

    eventually({
      val inputs = findAll(xpath("//input"))
      inputs.size mustBe 3
      inputs.foreach { input => {
        val t = new TextField(input)
        input.attribute("name") match {
          case Some("I_N") => t.value mustBe nsPlayer1
          case Some("I_S") => t.value mustBe nsPlayer2
          case Some("I_E") => t.value mustBe ewPlayer1
          case Some("I_W") => t.value mustBe ewPlayer2
          case x => log.fine( "Found an input field with name "+x )
        }
      }}
    })
  }

  def testNamesNoInput( nsPlayer1: String, nsPlayer2: String,
                        ewPlayer1: String, ewPlayer2: String
                      )(implicit webDriver: WebDriver, config: PatienceConfig, pos: Position): Unit = {
    eventually({
      val inputs1 = List("Player_N", "Player_S", "Player_E", "Player_W").map( p => {
        val e = findElemById(p)
        ( p, e)
      }).toMap

      inputs1.foreach { entry => {
        val (name, input) = entry
        val t = input.text
        name match {
          case "Player_N" => t mustBe nsPlayer1
          case "Player_S" => t mustBe nsPlayer2
          case "Player_E" => t mustBe ewPlayer1
          case "Player_W" => t mustBe ewPlayer2
          case x => log.fine( "Found a field with name "+x )
        }
      }}
    })
  }

}
