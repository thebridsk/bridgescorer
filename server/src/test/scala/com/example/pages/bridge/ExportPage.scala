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
import com.example.pages.GenericPage
import scala.reflect.io.File
import com.example.test.util.HttpUtils

object ExportPage {

  val log = Logger[ExportPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new ExportPage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor
    new ExportPage
  }

  def urlFor = TestServer.getAppPageUrl("export")

}

class ExportPage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[ExportPage] {
  import ExportPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor

    this
  }}

  /**
   * @param pos
   */
  def clickHome( implicit pos: Position ) = {
    clickButton("Home")
    new HomePage()(webDriver,pos)
  }


  /**
   * @param pos
   */
  def clickExport( implicit pos: Position ) = {
    clickButton("Export")
    new GenericPage()(webDriver,pos)
  }

  /**
   * @param file where to save the export.zip file
   * @param pos
   */
  def export( implicit pos: Position ): File = {
    val r = HttpUtils.getHttpAllBytesToFile( TestServer.getUrl("/v1/export") )
    if (r.status == 200) {
      r.data
    } else {
      fail(s"Error downloading export.zip: ${r}" )
    }
  }
}
