package com.github.thebridsk.bridge.server.test.pages

import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.browserpages.PageBrowser._
import org.openqa.selenium.WebDriver
import com.github.thebridsk.color.Color

trait LightDarkAddOn[+T <: Page[T]] {
  page: Page[T] =>

  import LightDarkAddOn._

  def validate(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    findElemById("""LightDark""")
    this
  }

  def clickLightDark(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    clickButton("LightDark")
    this
  }

  def checkIconLightMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = find( className("lightDarkIcon1"))
    val fill = toggleLightDark.cssValue("fill")
    withClue( s"Icon left color is ${fill}") {
      isDark( Color(fill))
    }
  }

  def checkIconDarkMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = find( className("lightDarkIcon1"))
    val fill = toggleLightDark.cssValue("fill")
    withClue( s"Icon left color is ${fill}") {
      isLight( Color(fill))
    }
  }

  def checkBodyLightMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = findElemByXPath( "//body" )
    val bg = toggleLightDark.cssValue("background-color")
    withClue( s"Body background color is ${bg}") {
      isLight( Color(bg))
    }
  }

  def checkBodyDarkMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = findElemByXPath( "//body" )
    val bg = toggleLightDark.cssValue("background-color")
    withClue( s"Body background color is ${bg}") {
      isDark( Color(bg))
    }
  }
}

object LightDarkAddOn {

  def isLight( c: Color ) = {
    val rgb = c.toRGBColor
    rgb.r > 200 && rgb.g > 200 && rgb.b > 200
  }

  def isDark( c: Color ) = {
    val rgb = c.toRGBColor
    rgb.r < 60 && rgb.g < 60 && rgb.b < 60
  }
}