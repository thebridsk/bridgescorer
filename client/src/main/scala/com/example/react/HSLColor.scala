package com.example.react

import org.scalajs.dom.ext.Color


/**
 * Converts an HSL color value to Color object. Conversion formula
 * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
 */
object HSLColor {

  /**
   * Converts an HSL color value to Color object.
   *
   * @param h  The hue, contained in the set [0, 360]
   * @param s  The saturation, contained in the set [0, 1]
   * @param l  The lightness, contained in the set [0, 1]
   * @return The Color object
   */
  def apply( h: Double, s: Double, l: Double ) = {
    val c = (1 - Math.abs(2*l-1))*s
    val mh = h%360
    val pmh = if (mh<0) mh+360 else mh
    val hp = pmh/60
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


}
