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
import com.example.pages.bridge.HomePage

object ListDuplicatePage {

  private[ListDuplicatePage] val log = Logger[ListDuplicatePage]

  val screenshotDir = "target/screenshots/PagesDuplicate"

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val importId = findUrlInfo
    new ListDuplicatePage(importId)
  }

  def goto( importId: Option[String] = None )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor(importId)
    new ListDuplicatePage(importId)
  }

  def urlFor( importId: Option[String] = None ) = TestServer.getAppPageUrl( importId.map(id=>s"import/${id}/").getOrElse("")+"duplicate")

  private val patternImport = """#(?:import/([^/]+)/)?duplicate""".r

  def findUrlInfo(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Option[String] = {
    val prefix = TestServer.getAppPage
    val cur = currentUrl
    withClue(s"Unable to determine import id: ${cur}") {
      cur must startWith (prefix)
      cur.drop( prefix.length() ) match {
        case patternImport(importId) =>
          Option(importId)
        case _ => fail(s"Could not determine if main or import store: ${cur}")
      }
    }
  }

  val buttons =
          "PopUpCancel"::
          "Home2"::
          "BoardSets2"::
          "Movements2"::
          "Home"::
          "ForPrint"::
          "DuplicateCreateTest"::
          "BoardSets"::
          "Movements"::
          "Pairs"::
          Nil

  val mainButtons =
          "DuplicateCreate"::
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

  val importSuccessPattern = """import duplicate (M\d+) from ([^,]+), new ID (M\d+)""".r

}

class ListDuplicatePage( importId: Option[String] )( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[ListDuplicatePage] {
  import ListDuplicatePage._

  val importColumns = importId.map( id => 1 ).getOrElse(0)

  def validate(implicit patienceConfig: PatienceConfig, pos: Position): ListDuplicatePage = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate ${patienceConfig}") {
    val b = importId.map( id => buttons ).getOrElse( buttons:::mainButtons)
    eventually{
      withClue(s"ListDuplicate.validate from ${pos.line}") {
        findButtons(b:_*)
        currentUrl mustBe urlFor(importId)
        assert( !isWorking )
      }
    }
    this
  }

  def validate( matchIds: String* )(implicit patienceConfig: PatienceConfig, pos: Position): ListDuplicatePage = {
    val b = importId.map( id => buttons ).getOrElse( buttons:::mainButtons)
    val allbuttons = (matchIds.map{ m => if (m.startsWith("M")) matchIdToButtonId(m) else resultIdToButtonId(m) }.toList:::b).toSet
    eventually{
      withClue(s"ListDuplicate.validate from ${pos.line}") {
        findAllButtons.keySet must contain allElementsOf (allbuttons)
        currentUrl mustBe urlFor(importId)
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

  def isForPrintActive( implicit pos: Position ) = {
    findAll(id("ForPrint")).isEmpty
  }

  def clickHome(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Home")
    new HomePage()(webDriver, pos)
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

  def clickForPrint(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("ForPrint")
    new ListDuplicatePage(importId)( webDriver, pos )
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
  def getNames( forPrintActive: Boolean )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val dr = 3+importColumns+(if (forPrintActive) 1 else 0)
    getElemsByXPath("""//div/table/thead/tr[3]/th""").drop(dr).map(e => e.text)
  }

  def getResults( id: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {

    withClueAndScreenShot(screenshotDir, "getResults", s"""working on results from match ${id}, ${pos.line}""") {
      eventually {
        val forPrintActive = isForPrintActive
        val row = getElemsByXPath(s"""//div/table/tbody/tr[td/button[@id='${matchIdToButtonId(id)}' or @id='${resultIdToButtonId(id)}']]/td""")
        val names = getNames(forPrintActive)
        val dr = 3+importColumns+(if (forPrintActive) 1 else 0)

        row.size mustBe names.size+dr+1

        (names zip row.drop(dr).map(e=>e.text)).map { case (name,result) => s"""${name}\n${result}""" }
      }
    }
  }

  def checkResults( id: String, results: String* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    withClueAndScreenShot(screenshotDir, "checkResults", s"""working on results from match ${id}, ${pos.line}, looking for ${results.mkString("[", "],[", "]")}""") {
      eventually {
        val res = getResults(id)
        results.foreach( r => res must contain (r))
        this
      }
    }
  }

  def isPopupDisplayed(implicit pos: Position) = {
    try {
      find( id("popup") ).isDisplayed
    } catch {
      case x: NoSuchElementException => false
    }
  }

  def validatePopup( visible: Boolean = true )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    eventually {
      isPopupDisplayed mustBe visible
    }
    this
  }

  def clickPopUpCancel( implicit pos: Position ) = {
    clickButton("PopUpCancel")
    this
  }

  def getPopUpText(implicit patienceConfig: PatienceConfig, pos: Position) = {
    find( className("baseDivPopupOKCancelBody")).text
  }


  def clickImportDuplicate( id: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (importId.isEmpty) fail( s"Not on import list duplicate page: ${currentUrl}" )
    clickButton( s"ImportDuplicate_${id}" )
  }

  /**
   * @param id the match being imported.  The Id of the match from the import store.
   * @return the Id of the imported match in the main store.
   * The call fails if the import was not successful
   */
  def checkSuccessfulImport( id: String )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    if (importId.isEmpty) fail( s"Not on import list duplicate page: ${currentUrl}" )
    isPopupDisplayed mustBe true
    val t = getPopUpText
    t match {
      case importSuccessPattern( oldId, importedId, newId ) =>
        oldId mustBe id
        importedId mustBe importId.get
        newId
      case _ =>
        fail(s"import failed: ${t}")
    }
  }

}
