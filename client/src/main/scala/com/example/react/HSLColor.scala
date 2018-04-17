package com.example.react

import org.scalajs.dom.ext.Color
import scala.collection.generic.CanBuildFrom
import japgolly.scalajs.react.vdom.Attr
import japgolly.scalajs.react.vdom.html_<^._

/**
 * Converts an HSL color value to Color object. Conversion formula
 * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
 *
 * @constructor
 * @param h  The hue, contained in the set [0, 360].  If not in range, modulo 360 is used.
 *           0 is red, 60 is yellow, 120 is green, 180 is cyan, 240 is blue, 300 is magenta
 * @param s  The saturation, contained in the set [0, 100].  If not in range, 0 or 100 is used.
 *           0 is gray (lack of color). 100 is full color.
 * @param l  The lightness, contained in the set [0, 100].  If not in range, 0 or 100 is used.
 *           0 is black, 100 is white.
 */
case class HSLColor( hue: Double, saturation: Double, lightness: Double ) {

  import HSLColor._

  def toColor =  {
    val h = modulo(hue,360)
    val s = toRange(saturation,0,100)/100
    val l = toRange(lightness,0,100)/100

    val c = (1 - Math.abs(2*l-1))*s
//    val mh = h%360
//    val pmh = if (mh<0) mh+360 else mh
    val hp = h/60
    val x = c*( 1 - Math.abs((hp%2) - 1))
    val (r1,g1,b1) = hp match {
      case hx if hx < 1 => (c,x,0.0)
      case hx if hx < 2 => (x,c,0.0)
      case hx if hx < 3 => (0.0,c,x)
      case hx if hx < 4 => (0.0,x,c)
      case hx if hx < 5 => (x,0.0,c)
      case hx if hx < 6 => (c,0.0,x)
      case hx =>
        (0.0,0.0,0.0)
    }
    val m = l - c/2
    val color = Color( Math.round((r1+m)*255).toInt, Math.round((g1+m)*255).toInt, Math.round((b1+m)*255).toInt )
//    println( f"""hsl($h%.0f,$s%.1f,$l%.1f): c=$c%.2f, hp=$hp%.0f, x=$x%.2f, m=$m%.2f, r1=$r1%.2f, g1=$g1%.2f, b1=$b1%.2f, rgb=$color""" )
    color
  }

  override
  def toString() = f"""HSLColor(h=${hue}%.0f,s=${saturation}%.1f,l=${lightness}%.1f)"""

  def toHsl = f"""hsl(${hue}%.0f,${saturation}%.1f%%,${lightness}%.1f%%)"""

  def attrColor = attr(^.color)

  def attrBackgroundColor = attr(^.backgroundColor)

  def attr( attribute: Attr[String] ) = attribute := toHsl
}

object HSLColor {
  import scala.language.implicitConversions

  implicit def hslColorToColor( hsl: HSLColor ) = hsl.toColor

  implicit def listHslColorToColor( list: Seq[HSLColor] ) = list.map(h => h.toColor)

  def toRange( v: Double, min: Double, max: Double ) = {
    Math.max( min, Math.min( v, max ) )
  }

  def modulo( v: Double, m: Double ) = {
    val r = v%m;
    if (r<0) r+m
    else r
  }

}
