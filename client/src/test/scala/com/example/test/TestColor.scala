package com.example.test

import org.scalatest.FlatSpec
import com.example.color._
import org.scalatest.MustMatchers

class TestColor extends FlatSpec with MustMatchers  {

  it should "convert between hsl, rgb and rgb%" in {

    for (r <- 0 to 255;
         g <- 0 to 255;
         b <- 0 to 255
    ) {
      val rgb = Color.rgb(r, g, b)

      testRgbHslRgb(rgb)
      testRgbPerRgb(rgb)
      testRgbHslPerRgb(rgb)
      testRgbPerHslRgb(rgb)
    }

  }

  it should "Convert rgb to attribute value and back" in {

    for (r <- 0 to 255 by 25;
         g <- 0 to 255 by 25;
         b <- 0 to 255 by 25;
         a <- 0 to 100 by 10
    ) {
      val rgb = Color.rgb(r, g, b, a)

      val s = rgb.toAttrValue
      val back = Color(s)
      rgb mustBe back
    }

  }

  it should "Convert rgb percent to attribute value and back" in {

    for (r <- 0 to 100 by 25;
         g <- 0 to 100 by 25;
         b <- 0 to 100 by 25;
         a <- 0 to 100 by 25
    ) {
      val rgb = Color.rgbPercent(r, g, b, a)

      val s = rgb.toAttrValue
      val back = Color(s)
      rgb mustBe back
    }

  }

  it should "Convert hsl to attribute value and back" in {

    for (h <- 0 to 360 by 60;
         s <- 0 to 100 by 25;
         l <- 0 to 100 by 25;
         a <- 0 to 100 by 25
    ) {
      val rgb = Color.hsl(h, s, l, a)

      val attr = rgb.toAttrValue
      val back = Color(attr)
      rgb mustBe back
    }

  }

  it should "convert a string to an RGBColor object" in {
    Color( "rgb( 255, 0, 0)" ) mustBe RGBColor(255,0,0)
    Color( "rgb( 100%, 0%, 0%)" ) mustBe RGBPercentColor(100,0,0)
    Color( "rgb( 0 255 0)" ) mustBe RGBColor(0,255,0)
    Color( "rgb( 100% 0% 0%)" ) mustBe RGBPercentColor(100,0,0)
    Color( "rgb( 255, 0, 0, .5)" ) mustBe RGBColor(255,0,0,50)
    Color( "rgb( 0%, 0%, 100%, 75%)" ) mustBe RGBPercentColor(0,0,100,75)
    Color( "rgb( 255 0 0 / 100%)" ) mustBe RGBColor(255,0,0,100)
    Color( "rgb( 100% 0% 0% / .5)" ) mustBe RGBPercentColor(100,0,0,50)
  }

  it should "convert a string to an HSLColor object" in {
    Color( "hsl( 0, 10%, 21%)" ) mustBe HSLColor(0,10,21)
    Color( "hsl( 60 50% 50%)" ) mustBe HSLColor(60,50,50)
    Color( "hsl( 90deg, 75%, 50%)" ) mustBe HSLColor(90,75,50)
    Color( "hsl( 100grad, 50%, 50%)" ) mustBe HSLColor(90,50,50)
    Color( "hsl( 1rad 50% 50% / .5)" ) mustBe HSLColor(1/Math.PI*180,50,50,50)
    Color( "hsl( .5turn, 50%, 50%, 50%)" ) mustBe HSLColor(180,50,50,50)
  }

  it should "get named colors" in {
    val red = Color("red")
    red mustBe NamedColor("red")
    red.toAttrValue mustBe "red"
    val redrgb = red.toRGBColor
    redrgb mustBe RGBColor(255,0,0)
    redrgb.toNamedColor mustBe NamedColor("red")
    val orange = Color("orange")
    orange mustBe NamedColor("orange")
    orange.toAttrValue mustBe "orange"
    val orangergb = orange.toRGBColor
    orangergb mustBe RGBColor(255,165,0)
    orangergb.toNamedColor mustBe NamedColor("orange")
  }

  def testRgbHslRgb( rgb: RGBColor ) = {
    val hsl = rgb.toRGBPercentColor
    val back = hsl.toRGBColor

    if (rgb != back) {
      fail( s"${rgb} did not convert back through hsl, ${hsl}, ${back}" )
    } else {
//        println( s"${rgb} ok, ${hsl}, ${back}" )
    }
  }

  def testRgbPerRgb( rgb: RGBColor ) = {
    val per = rgb.toRGBPercentColor
    val back = per.toRGBColor

    if (rgb != back) {
      fail( s"${rgb} did not convert back through percent, ${per}, ${back}" )
    } else {
//        println( s"${rgb} ok, ${hsl}, ${back}" )
    }
  }

  def testRgbHslPerRgb( rgb: RGBColor ) = {
    val hsl = rgb.toHSLColor
    val per = hsl.toRGBPercentColor
    val back = per.toRGBColor

    if (rgb != back) {
      fail( s"${rgb} did not convert back through hsl->per, ${hsl}, ${per}, ${back}" )
    } else {
//        println( s"${rgb} ok, ${hsl}, ${back}" )
    }
  }

  def testRgbPerHslRgb( rgb: RGBColor ) = {
    val per = rgb.toRGBPercentColor
    val hsl = per.toHSLColor
    val back = hsl.toRGBColor

    if (rgb != back) {
      fail( s"${rgb} did not convert back through per->hsl, ${per}, ${hsl}, ${back}" )
    } else {
//        println( s"${rgb} ok, ${hsl}, ${back}" )
    }
  }

}
