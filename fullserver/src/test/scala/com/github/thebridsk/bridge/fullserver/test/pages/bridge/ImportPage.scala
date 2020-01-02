package com.github.thebridsk.bridge.fullserver.test.pages.bridge

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page
import scala.reflect.io.File
import org.openqa.selenium.By.ByName
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ListDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.SummaryPage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ListPage

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

class ImportPage( implicit val webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[ImportPage] with Popup[ImportPage] {
  import ImportPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor

    this
  }}

  def validateSuccess( file: Option[File], beforeCount: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {
    currentUrl mustBe urlFor
    isPopupDisplayed mustBe false
    checkSelectedFile(file,beforeCount)
  }}


  def clickHome( implicit pos: Position ) = {
    clickButton("Home")
    new HomePage()(webDriver,pos)
  }

  def selectFile( file: File )( implicit pos: Position ) = {
    val upload = find(name("zip"))
    upload.sendKeys(file.toString);
    this
  }

  def checkSelectedFile( file: Option[File], beforeCount: Int )( implicit pos: Position ) = {
    val row = file.map { f =>
      val imports = getImportedIds
      imports.length mustBe beforeCount+1
      val foundImport = imports.find( i => i._1 == f.name)

      assert(foundImport.isDefined)
      foundImport.get._2
    }
    (this, row)
  }

  def isWorking(implicit pos: Position) = {
    findElemByXPath(HomePage.divBridgeAppPrefix+"//table/tbody/tr[2]/td[1]").text == "Working"
  }

  /**
   * @return list of tuple2.  Each tuple2 is (importId,row)
   */
  def getImportedIds(implicit patienceConfig: PatienceConfig, pos: Position) = eventually {
    val r = findElemsByXPath(HomePage.divBridgeAppPrefix+"//table/tbody/tr/td[1]").drop(1).map( e => e.text ).zipWithIndex
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

  def importChicago( importId: String, row: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton( s"Chicago${row}" )
    new ListPage( Some(importId) )
  }

  def importRubber( importId: String, row: Int )(implicit patienceConfig: PatienceConfig, pos: Position) = {
    clickButton( s"Rubber${row}" )
    new com.github.thebridsk.bridge.fullserver.test.pages.rubber.ListPage( Some(importId) )
  }
}
