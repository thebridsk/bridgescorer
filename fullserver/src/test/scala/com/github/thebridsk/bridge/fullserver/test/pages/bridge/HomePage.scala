package com.github.thebridsk.bridge.fullserver.test.pages.bridge

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.browserpages.GenericPage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ListDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.NewDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ListPage
import com.github.thebridsk.bridge.fullserver.test.pages.LightDarkAddOn
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.EnterNamesPage
import com.github.thebridsk.bridge.fullserver.test.pages.rubber
import org.scalatest.Assertion

object HomePage {

  val log: Logger = Logger[HomePage]()

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): HomePage = {
    new HomePage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): HomePage = {
    go to urlFor
    new HomePage
  }

  def demo(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): HomePage = {
    go to TestServer.getAppDemoPage
    new HomePage
  }

  def urlFor = TestServer.getAppPage

  val divBridgeAppPrefix = """//div[@id="BridgeApp"]"""
}

class HomePage( implicit webDrivr: WebDriver, pageCreated: SourcePosition ) extends Page[HomePage] with LightDarkAddOn[HomePage] with Popup[Page[HomePage]] {

  val webDriver = webDrivr

  def validate(implicit patienceConfig: PatienceConfig, pos: Position): HomePage = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
    eventually {
      pageTitle mustBe "The Bridge ScoreKeeper"
      findButton("Duplicate",Some("Duplicate List"))
    }
    this
  }

  def clickMainMenu(implicit patienceConfig: PatienceConfig, pos: Position): HomePage = {
    clickButton("MainMenu")
    this
  }

  def clickMoreMenu(implicit patienceConfig: PatienceConfig, pos: Position): HomePage = {
    clickButton("MoreMenu")
    this
  }

  def clickListDuplicateButton(implicit patienceConfig: PatienceConfig, pos: Position): ListDuplicatePage = {
    clickButton("Duplicate")
    new ListDuplicatePage(None)(webDriver, pos)
  }

  def clickNewDuplicateButton(implicit patienceConfig: PatienceConfig, pos: Position): NewDuplicatePage = {
    clickButton("NewDuplicate")
    new NewDuplicatePage()(webDriver, pos)
  }

  def clickLatestNewDuplicateButton(succeed: Boolean)(implicit patienceConfig: PatienceConfig, pos: Position): Page[_] = {
    clickButton("LatestNewMatch")
    if (succeed) {
      ScoreboardPage.waitFor
    } else {
      this
    }
  }

  def clickNewChicagoButton(implicit patienceConfig: PatienceConfig, pos: Position): EnterNamesPage = {
    clickButton("Chicago2")
    EnterNamesPage.currentWithId
  }

  def clickListChicagoButton(implicit patienceConfig: PatienceConfig, pos: Position): ListPage = {
    clickButton("ChicagoList2")
    new ListPage(None)(webDriver, pos)
  }

  def clickListRubberButton(implicit patienceConfig: PatienceConfig, pos: Position): rubber.ListPage = {
    clickButton("Rubber")
    new com.github.thebridsk.bridge.fullserver.test.pages.rubber.ListPage(None)(webDriver, pos)
  }

  def clickImport( implicit pos: Position ): ImportPage = {
    clickButton("Import")
    new ImportPage
  }

  def clickExport( implicit pos: Position ): ExportPage = {
    clickButton("Export")
    new ExportPage
  }

  def isMoreMenuVisible( implicit pos: Position ): Boolean = {
    try {
      val we = findElem( id("About") )
      true
    } catch {
      case x: Exception => false
    }

  }

  def clickHelp( implicit pos: Position ): GenericPage = {
    clickButton("Help")
    new GenericPage
  }

  def checkServers( urls: String* )(implicit patienceConfig: PatienceConfig, pos: Position): Assertion = {
    val urlsOnPage = getElemsByXPath("""//div[@id='url'/ul/li""").map( elem => elem.text )
    urlsOnPage.sorted mustBe urls.sorted
  }
}
