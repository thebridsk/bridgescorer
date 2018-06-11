package com.example.test.pages.bridge

import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.test.pages.PageBrowser._
import com.example.test.selenium.TestServer
import utils.logging.Logger
import com.example.test.pages.Page
import com.example.test.pages.GenericPage
import com.example.test.pages.duplicate.ListDuplicatePage
import com.example.test.pages.duplicate.NewDuplicatePage

object HomePage {

  val log = Logger[HomePage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new HomePage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor
    new HomePage
  }

  def urlFor = TestServer.getAppPage()

}

class HomePage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[HomePage] {
  import HomePage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
    eventually {
      pageTitle mustBe "The Bridge Score Keeper"
      findButton("Duplicate",Some("Duplicate List"))
    }
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

  def clickImport( implicit pos: Position ) = {
    clickButton("Import")
    new ImportPage
  }

  def clickExport( implicit pos: Position ) = {
    clickButton("Export")
    new ExportPage
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
