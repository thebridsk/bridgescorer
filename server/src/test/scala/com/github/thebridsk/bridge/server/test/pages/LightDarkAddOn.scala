package com.github.thebridsk.bridge.server.test.pages

import org.scalactic.source.Position
import org.scalatest.concurrent.Eventually._
import org.scalatest.MustMatchers._
import com.github.thebridsk.browserpages.Page
import com.github.thebridsk.browserpages.PageBrowser._
import org.openqa.selenium.WebDriver

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
      isDark( parseColor(fill))
    }
  }

  def checkIconDarkMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = find( className("lightDarkIcon1"))
    val fill = toggleLightDark.cssValue("fill")
    withClue( s"Icon left color is ${fill}") {
      isLight( parseColor(fill))
    }
  }

  def checkBodyLightMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = findElemByXPath( "//body" )
    val bg = toggleLightDark.cssValue("background-color")
    withClue( s"Body background color is ${bg}") {
      isLight( parseColor(bg))
    }
  }

  def checkBodyDarkMode(implicit webDriver: WebDriver, patienceConfig: PatienceConfig, pos: Position) = eventually {
    val toggleLightDark = findElemByXPath( "//body" )
    val bg = toggleLightDark.cssValue("background-color")
    withClue( s"Body background color is ${bg}") {
      isDark( parseColor(bg))
    }
  }
}

object LightDarkAddOn {

  val pFloat = """[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?"""
  val pInt = """\d+"""
  val patternRGB = s""" *rgba?\\( *(${pInt}) *, *(${pInt}) *, *(${pInt}) *(?:, *(${pFloat})(%?) *)?\\) *""".r

  case class Color( red: Int, green: Int, blue: Int, alpha: Option[Double] )

  def parseColor( s: String ) = {
    s match {
      case patternRGB(r,g,b,a,p) =>
        val alpha = if (a == null || a.length == 0) None
                    else Some(a.toDouble)
        val alphaN = alpha.map{ aa =>
          if (p == null || p.length == 0) aa
          else aa/100
        }
        Color(r.toInt,g.toInt,b.toInt,alphaN)
      case _ =>
        fail( s"Could not match to rgb[a] value: ${s}")
    }
  }

  def isLight( c: Color ) = {
    c.red > 200 && c.green > 200 && c.blue > 200
  }

  def isDark( c: Color ) = {
    c.red < 60 && c.green < 60 && c.blue < 60
  }
}