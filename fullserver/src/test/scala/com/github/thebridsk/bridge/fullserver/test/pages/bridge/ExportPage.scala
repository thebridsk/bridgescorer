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
import scala.reflect.io.File
import com.github.thebridsk.bridge.server.test.util.HttpUtils
import com.github.thebridsk.bridge.data.ImportStoreConstants

object ExportPage {

  val log: Logger = Logger[ExportPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): ExportPage = {
    new ExportPage
  }

  def goto(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): ExportPage = {
    go to urlFor
    new ExportPage
  }

  def urlFor: String = TestServer.getAppPageUrl("export")

}

class ExportPage(implicit webDriver: WebDriver, pageCreated: SourcePosition)
    extends Page[ExportPage] {
  import ExportPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): ExportPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        currentUrl mustBe urlFor

        this
      }
    }

  /**
    * @param pos
    */
  def clickHome(implicit pos: Position): HomePage = {
    clickButton("Home")
    new HomePage()(webDriver, pos)
  }

  /**
    * @param pos
    */
  def clickExport(implicit pos: Position): GenericPage = {
    clickButton("Export")
    new GenericPage()(webDriver, pos)
  }

  /**
    * @param file where to save the export.zip file
    * @param pos
    */
  def export(implicit pos: Position): File = {
    val r = HttpUtils.getHttpAllBytesToFile(TestServer.getUrl("/v1/export"))
    if (r.status == 200) {
      r.contentdisposition match {
        case Some(cd) =>
          if (
            !cd.contains("attachment") || !cd.contains(
              s"BridgeScorerExport.${ImportStoreConstants.importStoreFileExtension}"
            )
          ) {
            fail(
              s"Expecting content-disposition to contain attachment and BridgeScorerExport.zip, got ${cd}"
            )
          }
        case None =>
          fail("Expecting a content-disposition header in response")
      }
      r.data
    } else {
      fail(s"Error downloading export.zip: ${r}")
    }
  }
}
