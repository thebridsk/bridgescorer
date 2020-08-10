package com.github.thebridsk.bridge.fullserver.test.pages

import com.github.thebridsk.source.SourcePosition
import org.openqa.selenium.WebDriver
import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.PageBrowser._
import com.github.thebridsk.bridge.server.test.util.TestServer
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.browserpages.Page

object SkeletonPage {

  val log: Logger = Logger[SkeletonPage]()

  def current(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): SkeletonPage = {
    new SkeletonPage
  }

  def goto(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): SkeletonPage = {
    go to urlFor
    new SkeletonPage
  }

  def urlFor: String = TestServer.getAppPageUrl("duplicate/#new")

}

class SkeletonPage(implicit webDriver: WebDriver, pageCreated: SourcePosition)
    extends Page[SkeletonPage] {
  import SkeletonPage._

  def validate(implicit
      patienceConfig: PatienceConfig,
      pos: Position
  ): SkeletonPage =
    logMethod(s"${pos.line} ${getClass.getSimpleName}.validate") {
      eventually {

        currentUrl mustBe urlFor

        this
      }
    }

}
