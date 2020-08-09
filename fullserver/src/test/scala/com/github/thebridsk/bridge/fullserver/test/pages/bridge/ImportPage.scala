package com.github.thebridsk.bridge.fullserver.test.pages.bridge

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page
import scala.reflect.io.File
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.bridge.fullserver.test.pages.duplicate.ListDuplicatePage
import com.github.thebridsk.bridge.fullserver.test.pages.chicago.ListPage
import com.github.thebridsk.bridge.fullserver.test.pages.rubber

object ImportPage {

  val log: Logger = Logger[ImportPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): ImportPage = {
    new ImportPage
  }

  def goto(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): ImportPage = {
    go to urlFor
    new ImportPage
  }

  def urlFor: String = TestServer.getAppPageUrl("imports")

}

class ImportPage(implicit val webDriver: WebDriver, pageCreated: SourcePosition)
    extends Page[ImportPage]
    with Popup[ImportPage] {
  import ImportPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ImportPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        currentUrl mustBe urlFor

        this
      }
    }

  def validateSuccess(file: Option[File], beforeCount: Int)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): (ImportPage, Option[Int]) =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {
        currentUrl mustBe urlFor
        isPopupDisplayed mustBe false
        checkSelectedFile(file, beforeCount)
      }
    }

  def clickHome(implicit pos: Position): HomePage = {
    clickButton("Home")
    new HomePage()(webDriver, pos)
  }

  def selectFile(file: File)(implicit pos: Position): ImportPage = {
    val upload = find(name("zip"))
    upload.sendKeys(file.toString);
    this
  }

  def checkSelectedFile(file: Option[File], beforeCount: Int)(implicit
      pos: Position
  ): (ImportPage, Option[Int]) = {
    val row = file.map { f =>
      val imports = getImportedIds
      imports.length mustBe beforeCount + 1
      val foundImport = imports.find(i => i._1 == f.name)

      assert(foundImport.isDefined)
      foundImport.get._2
    }
    (this, row)
  }

  def isWorking(implicit pos: Position): Boolean = {
    findElemByXPath(
      HomePage.divBridgeAppPrefix + "//table/tbody/tr[2]/td[1]"
    ).text == "Working"
  }

  /**
    * @return list of tuple2.  Each tuple2 is (importId,row)
    */
  def getImportedIds(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): List[(String, Int)] =
    eventually {
      val r = findElemsByXPath(
        HomePage.divBridgeAppPrefix + "//table/tbody/tr/td[1]"
      ).drop(1).map(e => e.text).zipWithIndex
      if (r.length == 1) {
        r.head._1 must not be "Working"
      }
      r
    }

  def delete(
      row: Int
  )(implicit patienceConfig: PatienceConfig, pos: Position): ImportPage = {
    clickButton(s"Delete${row}")
    this
  }

  def importDuplicate(importId: String, row: Int)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListDuplicatePage = {
    clickButton(s"Duplicate${row}")
    new ListDuplicatePage(Some(importId))
  }

  def importChicago(importId: String, row: Int)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ListPage = {
    clickButton(s"Chicago${row}")
    new ListPage(Some(importId))
  }

  def importRubber(importId: String, row: Int)(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): rubber.ListPage = {
    clickButton(s"Rubber${row}")
    new com.github.thebridsk.bridge.fullserver.test.pages.rubber.ListPage(
      Some(importId)
    )
  }
}
