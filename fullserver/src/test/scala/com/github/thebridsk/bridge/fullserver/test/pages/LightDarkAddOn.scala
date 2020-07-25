package com.github.thebridsk.bridge.fullserver.test.pages

import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.matchers.must.Matchers._
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
    this.asInstanceOf[T]
  }

  def clickToLightDark(theme: Theme)(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    var i: Int = 3
    while (i > 0 && !theme.checkBody(false)) {
      val cur = getBodyBackgroundColor
      clickLightDark
      waitForBackgroundColorChange( cur )
      i=i-1
    }
    this.asInstanceOf[T]
  }

  /**
   * Clicks the LightDark button.  The themes go from:
   *   light -> medium -> dark
   */
  def clickLightDark(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    log.info("Clicking LightDark")
    clickButton("LightDark")
    this.asInstanceOf[T]
  }

  def checkIcon( theme: Theme )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    theme.checkIcon()
    this.asInstanceOf[T]
  }

  def checkBody( theme: Theme )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    theme.checkBody()
    this.asInstanceOf[T]
  }

}

object LightDarkAddOn {

  private val log = Logger[LightDarkAddOn[_]]()

  def getBodyBackgroundColor(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = {
    val toggleLightDark = find( xpath("//body")) // page.findElemByXPath( "//body" )
    toggleLightDark.cssValue("background-color")
  }

  def waitForBackgroundColorChange( current: String )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position): Unit = eventually {
    getBodyBackgroundColor must not be current
  }

  sealed trait Theme {
    def checkBody( throwException: Boolean = true )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
      val toggleLightDark = find( xpath("//body")) // page.findElemByXPath( "//body" )
      val bg = toggleLightDark.cssValue("background-color")
      withClue( s"Checking body for ${theme}, Body background color is ${bg}") {
        checkColor( Color(bg), throwException)
      }
    }

    def checkIcon( throwException: Boolean = true )(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
      val toggleLightDark = find( className("lightDarkIcon1"))
      val fill = toggleLightDark.cssValue("fill")
      log.info(s"Checking icon for medium: ${fill}")
      withClue( s"Checking body for ${theme}, Icon bottom color is ${fill}") {
        checkColor( Color(fill), throwException)
      }
    }

    def checkColor( c: Color, throwException: Boolean = true ): Boolean = {
      val rgb = c.toRGBColor
      if (throwException) {
        rgb mustBe color
        true
      } else {
        rgb == color
      }
    }

    val color: Color
    val theme: String
  }

  object LightTheme extends Theme {
    val color = Color("white").toRGBColor           // --color-bg
    val theme = "light"
  }

  object MediumTheme extends Theme {
    val color = Color("rgb(50,54,57)").toRGBColor  // --color-other-bg
    val theme = "medium"
  }

  object DarkTheme extends Theme {
    val color = Color("black").toRGBColor            // --color-other2-bg
    val theme = "dark"
  }

}
