package com.example.pages

import org.openqa.selenium.WebDriver
import com.example.source.SourcePosition
import com.example.pages.duplicate.ListDuplicatePage
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.pages.PageBrowser._
import com.example.pages.duplicate.NewDuplicatePage
import com.example.test.selenium.TestServer

class HomePage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[HomePage] {


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

  def checkServers( urls: String* )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    val urlsOnPage = getElemsByXPath("""//div[@id='url'/ul/li""").map( elem => elem.text )
    urlsOnPage.sorted mustBe urls.sorted
  }
}

object HomePage {

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new HomePage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to (TestServer.getAppPage)
    new HomePage
  }
}

