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

object ImportResultPage {

  val log = Logger[ImportResultPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new ImportResultPage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor
    new ImportResultPage
  }

  //http://localhost:8080/v1/import?url=http://localhost:8080/public/index-fastopt.html%23imports
  def urlFor = s"""${TestServer.hosturl}v1/import?url=${TestServer.getAppPage()}%23imports"""

}

class ImportResultPage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[ImportResultPage] {
  import ImportResultPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
    eventually {

      val cur = currentUrl
      withClue( s"""currentUrl is ${cur}\nurlfor is     ${urlFor}""" ) {
        log.info(s"""currentUrl is ${cur}\nurlfor is     ${urlFor}""")
        cur mustBe urlFor
      }

      this
    }
  }

  def isSuccessful(implicit patienceConfig: PatienceConfig, pos: Position) = {
    pageTitle == "Import Store"
  }

  def clickLink( implicit pos: Position ) = {
    find( xpath( "//body/p/a" ) ).click
    new ImportPage()(webDriver,pos)
  }
}