package com.github.thebridsk.bridge.fullserver.test.pages.individual

import com.github.thebridsk.browserpages.Page
import org.openqa.selenium.WebDriver
import com.github.thebridsk.source.SourcePosition
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import org.openqa.selenium.NoSuchElementException
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.Popup
import com.github.thebridsk.bridge.fullserver.test.pages.ServerURLAddOn
import com.github.thebridsk.bridge.data.MatchDuplicateResult
import com.github.thebridsk.bridge.data.MatchDuplicate
import scala.util.matching.Regex
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.BoardSetsPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.DuplicateResultPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.{ScoreboardPage => DScoreboardPage}

object ListDuplicatePage {

  private[ListDuplicatePage] val log = Logger[ListDuplicatePage]()

  val screenshotDir = "target/screenshots/PagesIndividual"

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    val importId = findUrlInfo
    new ListDuplicatePage(importId)
  }

  def waitFor(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = eventually { current }

  def goto(importId: Option[String] = None)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    go to urlFor(importId)
    new ListDuplicatePage(importId)
  }

  def urlFor(importId: Option[String] = None): String =
    TestServer.getAppPageUrl(
      importId.map(id => s"import/${id}/").getOrElse("") + "individual"
    )

  private val patternImport = """#(?:import/([^/]+)/)?individual""".r

  def findUrlInfo(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): Option[String] = {
    val prefix = TestServer.getAppPage
    val cur = currentUrl
    withClue(s"Unable to determine import id: ${cur}") {
      cur must startWith(prefix)
      cur.drop(prefix.length()) match {
        case patternImport(importId) =>
          Option(importId)
        case _ => fail(s"Could not determine if main or import store: ${cur}")
      }
    }
  }

  val buttons: List[String] =
//          "PopUpCancel"::
//          "Home2"::
    "Home" ::
      Nil

  val importButtons: List[String] =
    "ForImport" ::
      Nil

//          PopUpCancel,Home,DuplicateCreate,ForPrint
  val mainButtons: List[String] =
    "DuplicateCreate" ::
//          "BoardSets2"::
//          "Movements2"::
//          "ForPrint"::
//          "DuplicateCreateTest"::
//          "BoardSets"::
//          "Movements"::
//          "Statistics"::
      Nil

  val patternMatchButton: Regex = """Duplicate_(M\d+)""".r

  /**
    * @return None if unable to determine the match ID,
    *          Some(mid) if match id was found.
    */
  def buttonIdToMatchId(id: String): Option[String] =
    id match {
      case patternMatchButton(mid) => Some(mid)
      case _                       => None
    }

  def matchIdToButtonId(id: String): String = s"""Duplicate_${id}"""

  val patternIndividualButton: Regex = """Individual_(I\d+)""".r

  /**
    * @return None if unable to determine the match ID,
    *          Some(mid) if match id was found.
    */
  def buttonIdToIndividualId(id: String): Option[String] =
    id match {
      case patternIndividualButton(mid) => Some(mid)
      case _                       => None
    }

  def individualIdToButtonId(id: String): String = s"""Individual_${id}"""

  val patternResultButton: Regex = """Result_(E\d+)""".r

  /**
    * @return None if unable to determine the match ID,
    *          Some(mid) if match id was found.
    */
  def buttonIdToResultId(id: String): Option[String] =
    id match {
      case patternMatchButton(mid) => Some(mid)
      case _                       => None
    }

  def resultIdToButtonId(id: String): String = s"""Result_${id}"""

  val importSuccessPattern: Regex =
    """import duplicate (M\d+) from ([^,]+), new ID (M\d+)""".r

}

class ListDuplicatePage(
    importId: Option[String]
)(implicit
    val webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[ListDuplicatePage]
    with Popup[ListDuplicatePage]
    with ServerURLAddOn[ListDuplicatePage] {
  import ListDuplicatePage._

  val importColumns: Int = importId.map(id => 2).getOrElse(0)

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage =
    logMethod(
      s"${pos.line} ${getClass.getSimpleName}.validate ${patienceConfig}"
    ) {
      val b = importId
        .map(id => buttons ::: importButtons)
        .getOrElse(buttons ::: mainButtons)
      eventually {
        withClue(s"ListDuplicate.validate from ${pos.line}") {
          findButtons(b: _*)
          currentUrl mustBe urlFor(importId)
          assert(!isWorking)
        }
      }
      this
    }

  def validate(matchIds: String*)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    val b = importId
      .map(id => buttons ::: importButtons)
      .getOrElse(buttons ::: mainButtons)
    val allbuttons = (matchIds.map { m =>
      if (m.startsWith("M")) matchIdToButtonId(m)
      else if (m.startsWith("I")) individualIdToButtonId(m)
      else resultIdToButtonId(m)
    }.toList ::: b).toSet
    eventually {
      withClue(s"ListDuplicate.validate from ${pos.line}") {
        findAllButtons.keySet must contain allElementsOf (allbuttons)
        currentUrl mustBe urlFor(importId)
        assert(!isWorking)
      }
    }

    this
  }

  def isWorking(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    try {
      val text = find(
        xpath(HomePage.divBridgeAppPrefix + """//div/table/tbody/tr[1]/td[1]""")
      ).text
      val rc = text == "Working"
      log.fine(
        s"""Looking for working on duplicate list page, rc=${rc}: "${text}""""
      )
      rc
    } catch {
      case x: NoSuchElementException =>
        false
    }
  }

  def isForPrintActive(implicit pos: Position): Boolean = {
    // importId.isEmpty && findAll(id("ForPrint")).isEmpty
    if (importId.isEmpty) {
      val els = getElemsByXPath(
        HomePage.divBridgeAppPrefix + s"""//div/table/thead/tr[1]/th[2]"""
      )
      if (els.size == 1) {
        els.head.text.equals("Print")
      } else {
        false
      }
    } else {
      false
    }
  }

  def clickMainMenu(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    clickButton("MainMenu")
    this
  }

  def clickHelpMenu(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    clickButton("HelpMenu")
    this
  }

  def clickHome(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): HomePage = {
    clickButton("Home")
    new HomePage()(webDriver, pos)
  }

  def clickNewDuplicateButton(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): NewDuplicatePage = {
    clickButton("DuplicateCreate")
    new NewDuplicatePage(false)(webDriver, pos)
  }

  def clickBoardSets(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): BoardSetsPage = {
    clickButton("BoardSets")
    new BoardSetsPage()(webDriver, pos)
  }

  def clickMovements(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): IndividualMovementsPage = {
    clickButton("Movements")
    new IndividualMovementsPage(None, false)(webDriver, pos)
  }

  // def clickStatistics(implicit
  //     patienceConfig: PatienceConfig,
  //     pos: Position
  // ): StatisticsPage = {
  //   clickButton("Statistics")
  //   new StatisticsPage()(webDriver, pos)
  // }

  // def clickSuggestion(implicit
  //     patienceConfig: PatienceConfig,
  //     pos: Position
  // ): SuggestionPage = {
  //   clickButton("Suggest")
  //   new SuggestionPage()(webDriver, pos)
  // }

  def clickForPrint(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    clickButton("ForPrint")
    new ListDuplicatePage(importId)(webDriver, pos)
  }

  def clickDuplicate(
      id: String
  )(implicit patienceConfig: PatienceConfig, pos: Position): DScoreboardPage = {
    clickButton(matchIdToButtonId(id))
    new DScoreboardPage(Some(id))(webDriver, pos)
  }

  def clickIndividual(
      id: String
  )(implicit patienceConfig: PatienceConfig, pos: Position): ScoreboardPage = {
    clickButton(individualIdToButtonId(id))
    new ScoreboardPage(id)(webDriver, pos)
  }

  def clickResult(id: String)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): DuplicateResultPage = {
    clickButton(resultIdToButtonId(id))
    new DuplicateResultPage(Some(id))
  }

  def getMatchIds(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): List[MatchDuplicate.Id] = {
    getAllButtons.keySet
      .flatMap { id =>
        buttonIdToMatchId(id) match {
          case Some(mid) => MatchDuplicate.id(mid) :: Nil
          case None      => Nil
        }
      }
      .toList
      .sorted
  }

  def getResultIds(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): List[MatchDuplicateResult.Id] = {
    getAllButtons.keySet
      .flatMap { id =>
        buttonIdToResultId(id) match {
          case Some(mid) => MatchDuplicateResult.id(mid) :: Nil
          case None      => Nil
        }
      }
      .toList
      .sorted
  }

  /**
    * @return a sorted list of all the names that appear on the page
    */
  def getNames(
      forPrintActive: Boolean
  )(implicit patienceConfig: PatienceConfig, pos: Position): List[String] = {
    // 3 = Id, Created, Finished
    // Note the scoring method header does not show up in this row.
    val dr = 4 + importColumns + (if (forPrintActive) 1 else 0)
    val names = getElemsByXPath(
      HomePage.divBridgeAppPrefix + """//div/table/thead/tr/th"""
    ).drop(dr)
    names.dropRight(1).map(e => e.text)
  }

  def getResults(
      id: String
  )(implicit patienceConfig: PatienceConfig, pos: Position): List[String] = {

    withClueAndScreenShot(
      screenshotDir,
      "getResults",
      s"""working on results from match ${id}, ${pos.line}"""
    ) {
      eventually {
        val forPrintActive = isForPrintActive
        val row = getElemsByXPath(
          HomePage.divBridgeAppPrefix + s"""//div/table/tbody/tr[td/button[@id='${matchIdToButtonId(
            id
          )}' or @id='${resultIdToButtonId(id)}']]/td"""
        )
        val names = getNames(forPrintActive)
        // 4 = Id, Created, Finished, scoring method
        val dr = 4 + importColumns + (if (forPrintActive) 1 else 0)

        row.size mustBe names.size + dr + 1

        (names zip row.drop(dr).map(e => e.text)).map {
          case (name, result) => s"""${name}\n${result}"""
        }
      }
    }
  }

  def checkResults(id: String, results: String*)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    withClueAndScreenShot(
      screenshotDir,
      "checkResults",
      s"""working on results from match ${id}, ${pos.line}, looking for ${results
        .mkString("[", "],[", "]")}"""
    ) {
      eventually {
        val res = getResults(id)
        results.foreach(r => res must contain(r))
        this
      }
    }
  }

  def clickImportDuplicate(id: String)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    if (importId.isEmpty)
      fail(s"Not on import list duplicate page: ${currentUrl}")
    clickButton(s"ImportDuplicate_${id}")
  }

  /**
    * @param id the match being imported.  The Id of the match from the import store.
    * @return the Id of the imported match in the main store.
    * The call fails if the import was not successful
    */
  def checkSuccessfulImport(
      id: String
  )(implicit patienceConfig: PatienceConfig, pos: Position): String = {
    if (importId.isEmpty)
      fail(s"Not on import list duplicate page: ${currentUrl}")
    validatePopup(true)
    eventually {
      val t = getPopUpText
      t match {
        case importSuccessPattern(oldId, importedId, newId) =>
          oldId mustBe id
          importedId mustBe importId.get
          newId
        case _ =>
          fail(s"import failed: ${t}")
      }
    }
  }

}
