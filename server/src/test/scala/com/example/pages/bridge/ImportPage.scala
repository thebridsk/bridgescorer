package com.example.pages.bridge

import com.example.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.example.pages.PageBrowser._
import com.example.test.selenium.TestServer
import utils.logging.Logger
import com.example.pages.Page
import scala.reflect.io.File
import org.openqa.selenium.By.ByName
import org.scalatest.MustMatchers._
import com.example.pages.duplicate.ListDuplicatePage

object ImportPage {

  val log = Logger[ImportPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new ImportPage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor
    new ImportPage
  }

  def urlFor = TestServer.getAppPageUrl("imports")

}

class ImportPage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[ImportPage] {
  import ImportPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor

    this
  }}

  def clickHome( implicit pos: Position ) = {
    clickButton("Home")
    new HomePage()(webDriver,pos)
  }

  def clickImport( implicit pos: Position ) = {
    findInput("submit","submit").click
    new ImportResultPage()(webDriver,pos)
  }

  def selectFile( file: File )( implicit pos: Position ) = {
    val upload = find(name("zip"))
    upload.sendKeys(file.toString);
    this
  }

  def checkSelectedFile( file: Option[File] )( implicit pos: Position ) = {
    val t = findElemByXPath("//form/label").text
    file match {
      case Some(f) =>
        t mustBe s"Selected ${f.name}"
      case None =>
        t mustBe "Zipfile to import as a bridgestore"
    }
    this
  }

  def isWorking(implicit pos: Position) = {
    findElemByXPath("//table/tbody/tr[1]/td[1]").text == "Working"
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

  /**
   * @return list of tuple2.  Each tuple2 is (importId,row)
   */
  def getImportedIds(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    val r = findElemsByXPath("//table/tbody/tr/td[1]").map( e => e.text ).zipWithIndex
    if (r.length == 1) {
      r.head._1 must not be "Working"
    }
    r
  }

  def delete( row: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton( s"Delete${row}" )
    this
  }

  def importDuplicate( importId: String, row: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton( s"Duplicate${row}" )
    new ListDuplicatePage( Some(importId) )
  }
}
