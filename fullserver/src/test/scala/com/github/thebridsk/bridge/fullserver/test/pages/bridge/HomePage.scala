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
import com.github.thebridsk.browserpages.Element
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ScoreboardPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.EnterNamesPage

object HomePage {

  val log = Logger[HomePage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new HomePage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor
    new HomePage
  }

  def demo(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to TestServer.getAppDemoPage()
    new HomePage
  }

  def urlFor = TestServer.getAppPage()

  val divBridgeAppPrefix = """//div[@id="BridgeApp"]"""
}

class HomePage( implicit webDrivr: WebDriver, pageCreated: SourcePosition ) extends Page[HomePage] with LightDarkAddOn[HomePage] with Popup[Page[HomePage]] {
  import HomePage._

  val webDriver = webDrivr

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
    eventually {
      pageTitle mustBe "The Bridge ScoreKeeper"
      findButton("Duplicate",Some("Duplicate List"))
    }
    this
  }

  def clickMainMenu(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("MainMenu")
    this
  }

  def clickMoreMenu(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("MoreMenu")
    this
  }

  def clickListDuplicateButton(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Duplicate")
    new ListDuplicatePage(None)(webDriver, pos)
  }

  def clickNewDuplicateButton(implicit patienceConfig: PatienceConfig, pos: Position) = {
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

  def clickNewChicagoButton(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Chicago2")
    EnterNamesPage.currentWithId
  }

  def clickListChicagoButton(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("ChicagoList2")
    new ListPage(None)(webDriver, pos)
  }

  def clickListRubberButton(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("Rubber")
    new com.github.thebridsk.bridge.fullserver.test.pages.rubber.ListPage(None)(webDriver, pos)
  }

  def clickImport( implicit pos: Position ) = {
    clickButton("Import")
    new ImportPage
  }

  def clickExport( implicit pos: Position ) = {
    clickButton("Export")
    new ExportPage
  }

  def isMoreMenuVisible( implicit pos: Position ) = {
    try {
      val we = findElem( id("About") )
      true
    } catch {
      case x: Exception => false
    }

  }

  def clickHelp( implicit pos: Position ) = {
    clickButton("Help")
    new GenericPage
  }

  def checkServers( urls: String* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val urlsOnPage = getElemsByXPath("""//div[@id='url'/ul/li""").map( elem => elem.text )
    urlsOnPage.sorted mustBe urls.sorted
  }
}
