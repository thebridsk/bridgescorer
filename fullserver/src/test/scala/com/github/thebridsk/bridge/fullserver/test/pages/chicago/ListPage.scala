package com.github.thebridsk.bridge.fullserver.test.pages.chicago

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
import org.scalatest.Assertion
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
      s"""${importId.map(iid => s"imports/${iid}/").getOrElse("")}chicago"""
    )
  }

  val patternUrl: Regex = """(?:#import/([^/]+)/)?chicago""".r

  def getImportId(url: String): Option[String] = {
    val prefix = TestServer.getAppPage
    val prefix2 = TestServer.getAppDemoPage
    val test = if (url.startsWith(prefix)) {
      url.drop(prefix.length())
    } else if (url.startsWith(prefix2)) {
      url.drop(prefix2.length())
    } else {
      fail(s"""Url did not start with $prefix or $prefix2: $url""")
    }
    val r = test match {
      case patternUrl(iid) if iid != null && iid.length() > 0 => Some(iid)
      case _                                                  => None
    }
    log.fine(s"url=$url, test=$test, r=$r")
    r
  }

  val importSuccessPattern: Regex =
    """import chicago (C\d+) from ([^,]+), new ID (C\d+)""".r

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
      HomePage.divBridgeAppPrefix + """//div[contains(concat(' ', @class, ' '), ' chiChicagoListPage ')]//div/table/tbody/tr/td[1]/button"""
    ).map(e => e.text)
  }

  def getImportButtons()(implicit pos: Position): List[String] = {
    getElemsByXPath(
      HomePage.divBridgeAppPrefix + """//div/table/tbody/tr/td[2]/button"""
    ).flatMap(e => e.id.toList)
  }

  def checkMatchButton(id: String)(implicit pos: Position): ListPage = {
    val e = findButton(s"""Chicago${id}""")
    this
  }

  def checkImportButton(id: String)(implicit pos: Position): ListPage = {
    val e = findButton(s"""ImportChicago_${id}""")
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
      clickButton(s"ImportChicago_$id")
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
      fail(s"Not on import chicago summary page: ${currentUrl}")
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

  def getTable(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListTable = {
    val r = ListTable()
    log.fine(s"getTable: ${r}")
    r
  }

  /**
    * @param row the row, 0 based
    * @param players the values in the player cells
    * @param patienceConfig
    * @param pos
    */
  def checkPlayers(row: Int, players: String*)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): Assertion = {
    getTable.row(row).players must contain allElementsOf (players)
  }

  /**
    * @param row the row, 0 based
    * @param patienceConfig
    * @param pos
    */
  def clickDelete(
      row: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): ListPage = {
    val delete = find(
      xpath(
        s"//div[contains(concat(' ', @class, ' '), ' chiChicagoListPage ')]/table/tbody/tr[${row + 1}]/td[last()]"
      )
    )
    delete.click
    this
  }
}

object ListTable {
  def apply()(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListTable = {
    val cells = findAll(
      xpath(
        "//div[contains(concat(' ', @class, ' '), ' chiChicagoListPage ')]/table/tbody/tr"
      )
    ).map { row =>
      ListRow(row.findAll(xpath("./td")).map { cell =>
        cell.text
      })
    }
    new ListTable(cells)
  }
}

case class ListRow(cells: List[String]) {

  lazy val len = cells.length

  def id: String = cells.apply(0)
  def createdUpdated: String = cells(1)

  /**
    * @param i index, 0 based
    */
  def player(i: Int): Serializable = {
    val j = i + 2
    if (j < len - 1) cells(i)
    else
      new IndexOutOfBoundsException(
        s"ListRow has ${cells.length - 3} players, asking for index $i"
      )
  }

  def players: List[String] = {
    cells.take(len - 1).drop(2)
  }

  override def toString(): String = {
    s"ListRow(id=$id, cells=${cells.mkString(", ")})"
  }

}

case class ListTable(rows: List[ListRow]) {
  def row(row: Int): ListRow = rows(row)

  override def toString(): String = {
    s"ListTable${rows.map(_.toString()).mkString("[\n  ", "\n  ", "\n]")}"
  }
}
