package com.example.pages.duplicate

import com.example.pages.Page
import org.openqa.selenium.WebDriver
import com.example.source.SourcePosition
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.pages.PageBrowser._
import com.example.test.selenium.TestServer
import com.example.data.Id
import org.openqa.selenium.NoSuchElementException
import utils.logging.Logger
import com.example.pages.GenericPage

object ListDuplicatePage {

  private[ListDuplicatePage] val log = Logger[ListDuplicatePage]

  val screenshotDir = "target/screenshots/PagesDuplicate"

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new ListDuplicatePage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor
    new ListDuplicatePage
  }

  def urlFor = TestServer.getAppPageUrl("duplicate")

  val buttons =
          "PopUpCancel"::
          "Home2"::
          "BoardSets2"::
          "Movements2"::
          "DuplicateCreate"::
          "Home"::
          "ForPrint"::
          "DuplicateCreateTest"::
          "BoardSets"::
          "Movements"::
          "Pairs"::
          Nil

  val patternMatchButton = """Duplicate_(M\d+)""".r

  /**
   * @return None if unable to determine the match ID,
   *          Some(mid) if match id was found.
   */
  def buttonIdToMatchId( id: String ) = id match {
    case patternMatchButton(mid) => Some(mid)
    case _ => None
  }

  def matchIdToButtonId( id: String ) = s"""Duplicate_${id}"""

  val patternResultButton = """Result_(E\d+)""".r

  /**
   * @return None if unable to determine the match ID,
   *          Some(mid) if match id was found.
   */
  def buttonIdToResultId( id: String ) = id match {
    case patternMatchButton(mid) => Some(mid)
    case _ => None
  }

  def resultIdToButtonId( id: String ) = s"""Result_${id}"""
}

class ListDuplicatePage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[ListDuplicatePage] {
  import ListDuplicatePage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position): ListDuplicatePage = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate ${patienceConfig}") {
    eventually{
      withClue(s"ListDuplicate.validate from ${pos.line}") {
        findButtons(buttons:_*)
        currentUrl mustBe urlFor
        assert( !isWorking )
      }
    }
    this
  }

  def validate( matchIds: String* )(implicit patienceConfig: PatienceConfig, pos: Position): ListDuplicatePage = {
    val allbuttons = (matchIds.map{ m => if (m.startsWith("M")) matchIdToButtonId(m) else resultIdToButtonId(m) }.toList:::buttons).toSet
    eventually{
      withClue(s"ListDuplicate.validate from ${pos.line}") {
        findAllButtons.keySet must contain allElementsOf (allbuttons)
        currentUrl mustBe urlFor
        assert( !isWorking )
      }
    }

    this
  }

  def isWorking(implicit patienceConfig: PatienceConfig, pos: Position): Boolean = {
    try {
      val text = find(xpath("""//div/table/tbody/tr[1]/td[1]""")).text
      val rc = text == "Working"
      log.fine( s"""Looking for working on duplicate list page, rc=${rc}: "${text}"""" )
      rc
    } catch {
      case x: NoSuchElementException =>
        false
    }
  }

  def clickNewDuplicateButton(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("DuplicateCreate")
    new NewDuplicatePage()(webDriver, pos)
  }

  def clickBoardSets(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("BoardSets")
    new BoardSetsPage()(webDriver, pos)
  }

  def clickMovements(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Movements")
    new MovementsPage()(webDriver, pos)
  }

  def clickPairs(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Pairs")
    new PairsPage()(webDriver, pos)
  }

  def clickDuplicate( id: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(matchIdToButtonId(id))
    new ScoreboardPage(Some(id))(webDriver, pos)
  }

  def clickResult( id: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton(resultIdToButtonId(id))
    new DuplicateResultPage( Some(id) )
  }

  def getMatchIds(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getAllButtons.keySet.flatMap{ id => buttonIdToMatchId(id) match {
      case Some(mid) => mid::Nil
      case None => Nil
    }}.toList.sortWith((l,r)=> Id.idComparer(l, r)<0)
  }

  def getResultIds(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getAllButtons.keySet.flatMap{ id => buttonIdToResultId(id) match {
      case Some(mid) => mid::Nil
      case None => Nil
    }}.toList.sortWith((l,r)=> Id.idComparer(l, r)<0)
  }

  /**
   * @return a sorted list of all the names that appear on the page
   */
  def getNames(implicit patienceConfig: PatienceConfig, pos: Position) = {
    getElemsByXPath("""//div/table/thead/tr[3]/th""").drop(3).map(e => e.text)
  }

  def getResults( id: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {

    withClueAndScreenShot(screenshotDir, "getResults", s"""working on results from match ${id}, ${pos.line}""") {
      val row = getElemsByXPath(s"""//div/table/tbody/tr[td/button[@id='${matchIdToButtonId(id)}' or @id='${resultIdToButtonId(id)}']]/td""")
      val names = getNames

      row.size mustBe names.size+4

      (names zip row.drop(3).map(e=>e.text)).map { case (name,result) => s"""${name}\n${result}""" }
    }
  }

  def checkResults( id: String, results: String* ) = {
    val res = getResults(id)
    results.foreach( r => res must contain (r))
    this
  }

}
