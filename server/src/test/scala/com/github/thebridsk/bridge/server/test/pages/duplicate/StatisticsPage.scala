package com.github.thebridsk.bridge.server.test.pages.duplicate

import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.bridge.server.test.pages.bridge.HomePage
import org.openqa.selenium.NoSuchElementException
import javax.validation.constraints.AssertFalse
import com.github.thebridsk.bridge.server.test.pages.FullscreenAddOn

object StatisticsPage {
  val log = Logger[StatisticsPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new StatisticsPage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor
    new StatisticsPage
  }

  def urlFor = TestServer.getAppPageUrl("duplicate/stats")

  val buttons =
//    "Home"::
//    "Summary"::
//    "BoardSets"::
//    "Movements"::
    "ShowFilter"::
    "ShowPeopleDetails"::
    "ShowPairsDetails"::
    Nil
}

case class PeopleRow( name: String,
                      percentWon: String,
                      percentWonPoints: String,
                      percentMP: String,
                      normalizedIMP: String,
                      imps: String,
                      wonMP: String,
                      wonMPPoints: String,
                      WonImp: String,
                      WonImpPoints: String,
                      played: String,
                      playedMP: String,
                      playedIMP: String,
                      incomplete: String,
                      maxMPPer: String,
                      MP: String,
                      total: String
                    )

case class PeopleRowMP( name: String,
                        percentWon: String,
                        percentWonPoints: String,
                        percentMP: String,
                        wonMP: String,
                        wonMPPoints: String,
                        played: String,
                        incomplete: String,
                        maxMPPer: String,
                        MP: String,
                        total: String
                      )

case class PeopleRowIMP( name: String,
                         percentWon: String,
                         percentWonPoints: String,
                         normalizedIMP: String,
                         imps: String,
                         WonImp: String,
                         WonImpPoints: String,
                         played: String,
                         incomplete: String,
                       )

class StatisticsPage( implicit webDriver: WebDriver, pageCreated: SourcePosition )
    extends Page[StatisticsPage]
    with FullscreenAddOn[StatisticsPage]
{

  import StatisticsPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position): StatisticsPage = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate ${patienceConfig}") {
    eventually{
      withClue(s"ListDuplicate.validate from ${pos.line}") {
        findButtons(buttons:_*)
        currentUrl mustBe urlFor
        assert( !isWorking )
      }
    }
    this
  }

  def isWorking(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    try {
      val text = find(xpath(HomePage.divBridgeAppPrefix+"""//div/table/tbody/tr[1]/td[2]""")).text
      val rc = text == "Working"
      log.fine( s"""Looking for working on people page, rc=${rc}: "${text}"""" )
      rc
    } catch {
      case x: NoSuchElementException =>
        false
    }
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

  def clickHome(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Home")
    new HomePage()(webDriver, pos)
  }

  def clickSummary(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Summary")
    new ListDuplicatePage(None)(webDriver, pos)
  }

  def clickBoardSets(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("BoardSets")
    new BoardSetsPage()(webDriver, pos)
  }

  def clickMovements(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Movements")
    new MovementsPage()(webDriver, pos)
  }

  def clickPeopleResults(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("ShowPeopleResults")
    this
  }

  def clickPairsResults(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("ShowPairsResults")
    this
  }

  def clickPeopleDetails(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("ShowPeopleDetails")
    this
  }

  def clickPairsDetails(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("ShowPairsDetails")
    this
  }

  def getPlayerTableScoringStyle(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val button = getElemByXPath("""//div[contains(concat(' ', @class, ' '), ' dupViewPeopleTable ')]//button[contains(concat(' ', @class, ' '), ' baseButtonSelected ')]""")
    button.id
  }

  def clickPlayerTableScoringStyle( scoringMethod: String)(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val button = getElemByXPath(s"""//div[contains(concat(' ', @class, ' '), ' dupViewPeopleTable ')]//button[@id='${scoringMethod}']""")
    button.click
    this
  }

  def getPlayerTablePlayed(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getPlayerTableScoringStyle mustBe Some("CalcPlayed")
    getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' dupViewPeopleTable ')]/table/tbody/tr/td""").map(e=>e.text).grouped(17).map{ list =>
      PeopleRow(list(0),list(1),list(2),list(3),list(4),list(5),list(6),list(7),list(8),list(9),list(10),list(11),list(12),list(13),list(14),list(15),list(16))
    }.toList
  }

  def getPlayerTableMP(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getPlayerTableScoringStyle mustBe Some("CalcMP")
    getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' dupViewPeopleTable ')]/table/tbody/tr/td""").map(e=>e.text).grouped(11).map{ list =>
      PeopleRowMP(list(0),list(1),list(2),list(3),list(4),list(5),list(6),list(7),list(8),list(9),list(10))
    }.toList
  }

  def getPlayerTableIMP(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getPlayerTableScoringStyle mustBe Some("CalcIMP")
    getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' dupViewPeopleTable ')]/table/tbody/tr/td""").map(e=>e.text).grouped(9).map{ list =>
      PeopleRowIMP(list(0),list(1),list(2),list(3),list(4),list(5),list(6),list(7),list(8))
    }.toList
  }

  def checkPeople( players: PeopleRow* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val res = getPlayerTablePlayed
    log.fine( "Found the following on the people page:" )
    res.foreach( r => log.fine(s"""  ${r}"""))
    players.foreach( p =>
      withClue( s"""${pos.line} StatisticsPage: looking for ${p}""" ) {
        res must contain (p)
      }
    )
  }

  def checkPeopleMP( players: PeopleRowMP* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val res = getPlayerTableMP
    log.fine( "Found the following on the people page:" )
    res.foreach( r => log.fine(s"""  ${r}"""))
    players.foreach( p =>
      withClue( s"""${pos.line} StatisticsPage: looking for ${p}""" ) {
        res must contain (p)
      }
    )
  }

  def checkPeopleIMP( players: PeopleRowIMP* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val res = getPlayerTableIMP
    log.fine( "Found the following on the people page:" )
    res.foreach( r => log.fine(s"""  ${r}"""))
    players.foreach( p =>
      withClue( s"""${pos.line} StatisticsPage: looking for ${p}""" ) {
        res must contain (p)
      }
    )
  }
}
