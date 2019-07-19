package com.github.thebridsk.bridge.server.test.pages

import com.github.thebridsk.bridge.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.github.thebridsk.bridge.server.test.pages.PageBrowser._
import com.github.thebridsk.bridge.server.test.selenium.TestServer
import com.github.thebridsk.utilities.logging.Logger

object SkeletonPage {

  val log = Logger[SkeletonPage]

  def current(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    new SkeletonPage
  }

  def goto(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    go to urlFor
    new SkeletonPage
  }

  def urlFor = TestServer.getAppPageUrl("duplicate/#new")

}

class SkeletonPage( implicit webDriver: WebDriver, pageCreated: SourcePosition ) extends Page[SkeletonPage] {
  import SkeletonPage._

  def validate(implicit patienceConfig: PatienceConfig, pos: Position) = logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") { eventually {

    currentUrl mustBe urlFor

    this
  }}

}
