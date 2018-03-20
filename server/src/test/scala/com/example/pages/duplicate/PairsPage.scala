package com.example.pages.duplicate

import utils.logging.Logger
import com.example.pages.Page
import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.pages.PageBrowser._
import com.example.test.selenium.TestServer
import com.example.pages.bridge.HomePage
import org.openqa.selenium.NoSuchElementException
import javax.validation.constraints.AssertFalse

object PairsPage {
  val log = Logger[PairsPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new PairsPage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor
    new PairsPage
  }

  def urlFor = TestServer.getAppPageUrl("duplicate/pairs")

  val buttons = "Home"::
                "Summary"::
                "BoardSets"::
                "Movements"::
                "ShowFilter"::
                "ShowPeopleDetails"::
                "ShowPairsDetails"::
                Nil
}

case class PeopleRow( name: String,
                      percentWon: String,
                      percentWonPoints: String,
                      percentMP: String,
                      imps: String,
                      wonMP: String,
                      wonMPPoints: String,
                      WonImp: String,
                      WonImpPoints: String,
                      played: String,
                      playedMP: String,
                      playedIMP: String,
                      incomplete: String,
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
                        MP: String,
                        total: String
                      )

case class PeopleRowIMP( name: String,
                         percentWon: String,
                         percentWonPoints: String,
                         imps: String,
                         WonImp: String,
                         WonImpPoints: String,
                         played: String,
                         incomplete: String,
                       )

class PairsPage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[PairsPage] {

  import PairsPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position): PairsPage = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate ${patienceConfig}") {
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
      val text = find(xpath("""//div/table/tbody/tr[1]/td[2]""")).text
      val rc = text == "Working"
      log.fine( s"""Looking for working on people page, rc=${rc}: "${text}"""" )
      rc
    } catch {
      case x: NoSuchElementException =>
        false
    }
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
    getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' dupViewPeopleTable ')]/table/tbody/tr/td""").map(e=>e.text).grouped(15).map{ list =>
      PeopleRow(list(0),list(1),list(2),list(3),list(4),list(5),list(6),list(7),list(8),list(9),list(10),list(11),list(12),list(13),list(14))
    }.toList
  }

  def getPlayerTableMP(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getPlayerTableScoringStyle mustBe Some("CalcMP")
    getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' dupViewPeopleTable ')]/table/tbody/tr/td""").map(e=>e.text).grouped(10).map{ list =>
      PeopleRowMP(list(0),list(1),list(2),list(3),list(4),list(5),list(6),list(7),list(8),list(9))
    }.toList
  }

  def getPlayerTableIMP(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getPlayerTableScoringStyle mustBe Some("CalcIMP")
    getElemsByXPath("""//div[contains(concat(' ', @class, ' '), ' dupViewPeopleTable ')]/table/tbody/tr/td""").map(e=>e.text).grouped(8).map{ list =>
      PeopleRowIMP(list(0),list(1),list(2),list(3),list(4),list(5),list(6),list(7))
    }.toList
  }

  def checkPeople( players: PeopleRow* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val res = getPlayerTablePlayed
    log.fine( "Found the following on the people page:" )
    res.foreach( r => log.fine(s"""  ${r}"""))
    players.foreach( p =>
      withClue( s"""${pos.line} PairsPage: looking for ${p}""" ) {
        res must contain (p)
      }
    )
  }

  def checkPeopleMP( players: PeopleRowMP* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val res = getPlayerTableMP
    log.fine( "Found the following on the people page:" )
    res.foreach( r => log.fine(s"""  ${r}"""))
    players.foreach( p =>
      withClue( s"""${pos.line} PairsPage: looking for ${p}""" ) {
        res must contain (p)
      }
    )
  }

  def checkPeopleIMP( players: PeopleRowIMP* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val res = getPlayerTableIMP
    log.fine( "Found the following on the people page:" )
    res.foreach( r => log.fine(s"""  ${r}"""))
    players.foreach( p =>
      withClue( s"""${pos.line} PairsPage: looking for ${p}""" ) {
        res must contain (p)
      }
    )
  }
}
