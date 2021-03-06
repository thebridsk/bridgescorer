package com.github.thebridsk.bridge.fullserver.test.pages.rubber

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.browserpages.PagesAssertions
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.Popup
import com.github.thebridsk.bridge.fullserver.test.pages.bridge.HomePage
import scala.util.matching.Regex

object ListPage {

  val log: Logger = Logger[ListPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListPage = {
    val url = currentUrl
    val id = getImportId(url)
    new ListPage(id)
  }

  def goto(importId: Option[String] = None)(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListPage = {
    go to urlFor(importId)
    new ListPage(importId)
  }

  //http://localhost:8080/v1/import?url=http://localhost:8080/public/index-fastopt.html%23imports
  def urlFor(importId: Option[String] = None): String = {
    TestServer.getAppPageUrl(
      s"""${importId.map(iid => s"imports/${iid}/").getOrElse("")}rubber"""
    )
  }

  val patternUrl: Regex = """(?:#import/([^/]+)/)?rubber""".r

  def getImportId(url: String): Option[String] = {
    val prefix = TestServer.getAppPage
    val test = if (url.startsWith(prefix)) {
      url.substring(prefix.length())
    } else {
      fail(s"""Url did not start with $prefix: $url""")
    }
    val r = test match {
      case patternUrl(iid) if iid != null && iid.length() > 0 => Some(iid)
      case _                                                  => None
    }
    log.fine(s"url=$url, prefix=$prefix, test=$test, r=$r")
    r
  }

  val importSuccessPattern: Regex =
    """import rubber (R\d+) from ([^,]+), new ID (R\d+)""".r

}

class ListPage(importId: Option[String] = None)(implicit
    val webDriver: WebDriver,
    pageCreated: SourcePosition
) extends Page[ListPage]
    with Popup[ListPage]
    with PagesAssertions {
  import ListPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        val cur = currentUrl
        val id = getImportId(cur)
        withClue(s"""currentUrl is ${cur}\ntarget id is ${importId}""") {
          log.info(s"""currentUrl is ${cur}\ntarget id is ${importId}""")
          id mustBe importId
        }

        this
      }
    }

  def validate(
      id: String
  )(implicit patienceConfig: PatienceConfig, pos: Position): ListPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      validate(patienceConfig, pos)
      checkMatchButton(id)
      this
    }

  def getMatchButtons()(implicit pos: Position): List[String] = {
    getElemsByXPath(
      HomePage.divBridgeAppPrefix + """//div/table/tbody/tr/td[1]/button"""
    ).map(e => e.text)
  }

  def getImportButtons()(implicit pos: Position): List[String] = {
    getElemsByXPath(
      HomePage.divBridgeAppPrefix + """//div/table/tbody/tr/td[2]/button"""
    ).flatMap(e => e.id.toList)
  }

  def checkMatchButton(id: String)(implicit pos: Position): ListPage = {
    val e = findButton(s"""Rubber${id}""")
    this
  }

  def checkImportButton(id: String)(implicit pos: Position): ListPage = {
    val e = findButton(s"""ImportRubber_${id}""")
    this
  }

  def clickHome(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): HomePage = {
    clickButton("Home")
    new HomePage()(webDriver, pos)
  }

  def clickImport(id: String)(implicit pos: Position): ListPage = {
    withClueEx(s"${pos.line}: trying to import ${id}") {
      clickButton(s"ImportRubber_$id")
    }
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
      fail(s"Not on import rubber summary page: ${currentUrl}")
    eventually {
      validatePopup(true)
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
