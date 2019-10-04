package com.github.thebridsk.bridge.server.test.pages

import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.browserpages.PageBrowser._
import org.openqa.selenium.WebDriver
import com.github.thebridsk.color.Color
import com.github.thebridsk.browserpages.GenericPage

trait FullscreenAddOn {

  def clickFullscreen(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    implicit val page = GenericPage.current
    page.clickButton("Fullscreen")
    this
  }
}
