package com.github.thebridsk.bridge.server.test.pages

import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.browserpages.PageBrowser._
import org.openqa.selenium.WebDriver
import com.github.thebridsk.color.Color
import com.github.thebridsk.utilities.logging.Logger

trait LightDarkAddOn[+T <: Page[T]] {
  page: Page[T] =>

  import LightDarkAddOn._

  def validateLightDark(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    findElemById("""LightDark""")
    this
  }

  /**
   * Clicks the LightDark button.  The themes go from:
   *   light -> medium -> dark
   */
  def clickLightDark(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    log.info("Clicking LightDark")
    clickButton("LightDark")
    this
  }

  def checkIconLightMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = find( className("lightDarkIcon1"))
    val fill = toggleLightDark.cssValue("fill")
    log.info(s"Checking icon for light: ${fill}")
    withClue( s"Icon bottom color is ${fill}") {
      isLight( Color(fill))
    }
  }

  def checkIconMediumMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = find( className("lightDarkIcon1"))
    val fill = toggleLightDark.cssValue("fill")
    log.info(s"Checking icon for medium: ${fill}")
    withClue( s"Icon bottom color is ${fill}") {
      isMedium( Color(fill))
    }
  }

  def checkIconDarkMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = find( className("lightDarkIcon1"))
    val fill = toggleLightDark.cssValue("fill")
    log.info(s"Checking icon for dark: ${fill}")
    withClue( s"Icon bottom color is ${fill}") {
      isDark( Color(fill))
    }
  }

  def checkBodyLightMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = findElemByXPath( "//body" )
    val bg = toggleLightDark.cssValue("background-color")
    log.info(s"Checking body for light: ${bg}")
    withClue( s"Body background color is ${bg}") {
      isLight( Color(bg))
    }
  }

  def checkBodyMediumMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = findElemByXPath( "//body" )
    val bg = toggleLightDark.cssValue("background-color")
    log.info(s"Checking body for medium: ${bg}")
    withClue( s"Body background color is ${bg}") {
      isMedium( Color(bg))
    }
  }

  def checkBodyDarkMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = findElemByXPath( "//body" )
    val bg = toggleLightDark.cssValue("background-color")
    log.info(s"Checking body for dark: ${bg}")
    withClue( s"Body background color is ${bg}") {
      isDark( Color(bg))
    }
  }
}

object LightDarkAddOn {

  val log = Logger[LightDarkAddOn[_]]

  // The following colors must match the values in bridge.css
  val light = Color("white").toRGBColor           // --color-bg
  val medium = Color("rgb(50,54,57)").toRGBColor  // --color-other-bg
  val dark = Color("black").toRGBColor            // --color-other2-bg

  def isLight( c: Color ) = {
    val rgb = c.toRGBColor
    rgb mustBe light
  }

  def isMedium( c: Color ) = {
    val rgb = c.toRGBColor
    rgb mustBe medium
  }

  def isDark( c: Color ) = {
    val rgb = c.toRGBColor
    rgb mustBe dark
  }
}
