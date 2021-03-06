package com.github.thebridsk.bridge.fullserver.test.pages

import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.browserpages.PageBrowser._
import org.openqa.selenium.WebDriver

trait FullscreenAddOn[+T <: Page[T]] {
  page: Page[T] =>

  def hasFullscreenButton(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): Boolean = {
    try {
      webDriver.findElement(id("Fullscreen").query)
      true
    } catch {
      case x: org.openqa.selenium.NoSuchElementException =>
        false
      case x: Exception =>
        fail("Unknown error finding button Fullscreen", x)
    }
  }

  def clickFullscreen(implicit
      webDriver: WebDriver,
      patienceConfig: PatienceConfig,
      pos: Position
  ): T = {
    clickButton("Fullscreen")
    this.asInstanceOf[T]
  }
}
