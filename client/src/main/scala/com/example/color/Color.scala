package com.example.color

import japgolly.scalajs.react.vdom.Attr.ValueType

trait Color {
  def toAttrValue: String

  def toHSLColor: HSLColor

  def toRGBColor: RGBColor

  def toRGBPercentColor: RGBPercentColor

  def toNamedColor: NamedColor
}

object ColorInternal {

  val pHex = "[0-9a-fA-F]"
  val pFloat = """[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?"""
  val pInt = """\d+"""
  val patternRGB = s""" *rgba?\\( *(${pInt}) *, *(${pInt}) *, *(${pInt}) *(?:, *(${pFloat})(%?) *)?\\) *""".r
  val patternRGBs = s""" *rgba?\\( *(${pInt}) +(${pInt}) +(${pInt}) *(?:/ *(${pFloat})(%?) *)?\\) *""".r

  val patternRGBp = s"""rgba?\\( *(${pFloat})% *, *(${pFloat})% *, *(${pFloat})% *(?:, *(${pFloat})(%?) *)?\\) *""".r
  val patternRGBps = s"""rgba?\\( *(${pFloat})% +(${pFloat})% +(${pFloat})% *(?:/ *(${pFloat})(%?) *)?\\) *""".r

  val patternLongHex = s"""#(${pHex}${pHex})(${pHex}${pHex})(${pHex}${pHex})(${pHex}${pHex})?""".r
  val patternShortHex = s"""#(${pHex})(${pHex})(${pHex})(${pHex})?""".r

  // deg grad rad turn    https://drafts.csswg.org/css-values-3/#angle-value

  val patternHSL = s"""hsla?\\( *(${pFloat})(deg|grad|rad|turn)? *, *(${pFloat})% *, *(${pFloat})% *(?:, *(${pFloat})(%?) *)?\\) *""".r
  val patternHSLs = s"""hsla?\\( *(${pFloat})(deg|grad|rad|turn)? +(${pFloat})% +(${pFloat})% *(?:/ *(${pFloat})(%?) *)?\\) *""".r

  def processA( a: String, p: String ) = {
    if (a == null) 100
    else if (p == null || p == "") a.toDouble*100
    else a.toDouble
  }

  def processAngle( h: String, d: String ) = {
    val hue = h.toDouble
    d match {
      case "deg" | "" | null => hue
      case "grad" => hue/100*90
      case "rad" => hue/Math.PI*180
      case "turn" => hue*360
      case _ => hue     // can't happen
    }
  }

  def hex( v: String ) = {
    Integer.parseInt( v, 16 )
  }

  def processHexA( v: String ) = {
    if (v == null) 100
    else hex(v)/2.55
  }

  /**
   * returns a string that represents the alpha value in rgb and hsl functions.
   * @param a the alpha value as a percent
   */
  def aToString( a: Double ) = {
    if (a == 100.0) ""
    else f",$a%.2f%%"
  }

  /**
   * returns a string that represents the alpha value in rgb and hsl functions.
   * @param a the alpha value as a percent
   */
  def aToStringForSpace( a: Double ) = {
    if (a == 100.0) ""
    else f"/$a%.2f%%"
  }

  /**
   * returns a string that represents the alpha value in rgb and hsl functions.
   * @param a the alpha value as a percent
   */
  def aToHex( a: Double ) = {
    if (a == 100.0) ""
    else {
      val b = Math.round( a/100*255 )
      val c = if (b < 0) 0
      else if (b>255) 255
      else b
      f"${c}%2X"
    }
  }
}

object Color {
  import ColorInternal._

  /**
   * Converts a CSS color string to a Color object.  If named colors are set,
   * then the color name must be valid.  To create a named color without checking for validity, use the #named method
   * Support CSS Color level 3 https://drafts.csswg.org/css-color-3/
   * Some support for CSS Color Module Level 4 https://drafts.csswg.org/css-color/#colorunits
   *
   * Supported formats for string:
   *   currentColor
   *   transparent
   *   <a color name>
   *   rgb( r g b )          r g b as either integers 0-255 or float% [0,100]
   *   rgb( r g b / a )      a as float [0,1] or float% [0,100]
   *   rgb( r, g, b )
   *   rgb( r, g, b, a )
   *   rgba( r g b )
   *   rgba( r g b / a )
   *   rgba( r, g, b )
   *   rgba( r, g, b, a )
   *   #rgb                   r g b a are hex digits.
   *   #rgba
   *   #rrggbb
   *   #rrggbbaa
   *   hsl( h s l )           h as float with deg, grad, rad, turn
   *   hsl( h s l / a )       s l as float% [0,100]
   *   hsl( h, s, l )         a as float [0,1] or float% [0,100]
   *   hsl( h, s, l, a )
   *   hsla( h s l )
   *   hsla( h s l / a )
   *   hsla( h, s, l )
   *   hsla( h, s, l, a )
   *
   * @param v the CSS color string.
   * @return the appropriate Color object.
   * @throws IllegalArgumentException if unable to parse v
   */
  def apply( v: String ): Color = {
    v match {
      case patternRGB(r,g,b,a,p) => rgb(r.toInt,g.toInt,b.toInt,processA(a,p))
      case patternRGBs(r,g,b,a,p) => rgb(r.toInt,g.toInt,b.toInt,processA(a,p))
      case patternRGBp(r,g,b,a,p) => rgbPercent(r.toDouble,g.toDouble,b.toDouble,processA(a,p))
      case patternRGBps(r,g,b,a,p) => rgbPercent(r.toDouble,g.toDouble,b.toDouble,processA(a,p))
      case patternHSL(h,d,s,l,a,p) => hsl( processAngle(h,d), s.toDouble, l.toDouble, processA(a,p) )
      case patternHSLs(h,d,s,l,a,p) => hsl( processAngle(h,d), s.toDouble, l.toDouble, processA(a,p) )
      case patternLongHex(r,g,b,a) => rgb( hex(r), hex(g), hex(b), processHexA(a) )
      case patternShortHex(r,g,b,a) => rgb( hex(r+r), hex(g+g), hex(b+b), processHexA(a) )
      case n: String if NamedColor.isNameValid(n) => named(n)
      case _ =>
        throw new IllegalArgumentException(s"Unknown color value: $v")
    }
  }

  /**
   * returns a Color object using RGB to construct it.
   *
   * @param r  The red, in the range [0,255]
   * @param g  The green, in the range [0,255]
   * @param b  The blue, in the range [0,255]
   * @param a  The alpha as a percent.  0 - transparent and 100 full opacity
   */
  def rgb( r: Int, g: Int, b: Int, a: Double = 100 ) = {
    new RGBColor(r,g,b,a)
  }

  /**
   * returns a Color object using RGB to construct it.
   *
   * @param r  The red as a percent, in the range [0,100]
   * @param g  The green as a percent, in the range [0,100]
   * @param b  The blue as a percent, in the range [0,100]
   * @param a  The alpha as a percent.  0 - transparent and 100 full opacity
   */
  def rgbPercent( r: Double, g: Double, b: Double, a: Double = 100 ) = {
    new RGBPercentColor(r,g,b,a)
  }

  /**
   * returns a Color object using HSL to construct it.
   *
   * @param hue  The hue, contained in the set [0, 360].  If not in range, modulo 360 is used.
   *           0 is red, 60 is yellow, 120 is green, 180 is cyan, 240 is blue, 300 is magenta
   * @param saturation  The saturation as a percent, contained in the set [0, 100].  If not in range, 0 or 100 is used.
   *           0 is gray (lack of color). 100 is full color.
   * @param lightness  The lightness as a percent, contained in the set [0, 100].  If not in range, 0 or 100 is used.
   *           0 is black, 100 is white.
   * @param a  The alpha as a percent.  0 - transparent and 100 full opacity
   */
  def hsl( h: Double, s: Double, l: Double, a: Double = 100 ) = {
    new HSLColor(h,s,l,a)
  }


/**
 * Support a named color.  Also the pseudo colors transparent, currentColor.
 *
 * See https://developer.mozilla.org/en-US/docs/Web/CSS/color_value
 *
 * @param s the name, the name is not validated.
 */
  def named( s: String ) = NamedColor(s)

  /**
   * @param gray 0 is black, 100 is white
   * @param alpha the alpha channel value as a percent, 0 is transparent, 100 is opaque
   */
  def grayscale( v: Double, a: Double = 100 ) = rgbPercent( v, v, v, a )

  val CurrentColor = NamedColor("currentColor")
  val Transparent = NamedColor("transparent")

  val Black = Color("black")
  val Silver = Color("silver")
  val Gray = Color("gray")
  val White = Color("white")
  val Maroon = Color("maroon")
  val Red = Color("red")
  val Purple = Color("purple")
  val Fuchsia = Color("fuchsia")
  val Green = Color("green")
  val Lime = Color("lime")
  val Olive = Color("olive")
  val Yellow = Color("yellow")
  val Navy = Color("navy")
  val Blue = Color("blue")
  val Teal = Color("teal")
  val Aqua = Color("aqua")
  val Orange = Color("orange")
  val Cyan = Color("cyan")        // same as aqua
  val Magenta = Color("magenta")  // same as fuchsia
  val Grey = Color("grey")        // same as grey

  val all = Seq(
      Black,
      Silver,
      Gray,
      White,
      Maroon,
      Red,
      Purple,
      Fuchsia,
      Green,
      Lime,
      Olive,
      Yellow,
      Navy,
      Blue,
      Teal,
      Aqua,
      Orange,
      Cyan,
      Magenta,
      Grey
      )

  /**
   * @return all the known named colors.
   */
  def allNamedColors: Iterable[String] = NamedColor.namedColors.keys

  /**
   * This implicit value allows the direct assignment to an attribute.
   * For example:
   *
   *     ^.color := Color("red")
   */
  implicit val vdomAttrColor: ValueType[Color, String] =
    ValueType((b, a) => b(a.toAttrValue))

}

/**
 * Support a named color.  Also the pseudo colors transparent, currentColor.
 *
 * See https://developer.mozilla.org/en-US/docs/Web/CSS/color_value
 *
 * @param name
 */
case class NamedColor( name: String ) extends Color {

  def toAttrValue = name

  def toRGBColor = NamedColor.toRGB(name)

  def toRGBPercentColor = toRGBColor.toRGBPercentColor

  def toHSLColor = toRGBColor.toHSLColor

  def toNamedColor = this
}

object NamedColor {

  def toRGB( name: String ): RGBColor = {
    namedColors.get(name.toLowerCase) match {
      case Some( v ) => Color(v).asInstanceOf[RGBColor]
      case None => throw new IllegalArgumentException(s"Name not a valid named color: ${name}")
    }
  }

  def toNamed( color: Color ): NamedColor = {
    if (color.isInstanceOf[NamedColor]) color.asInstanceOf[NamedColor]
    else {
      val rgb = color.toRGBColor
      if (rgb.a == 100) {
        val attr = f"#${rgb.r}%02x${rgb.g}%02x${rgb.b}%02x"
        val entry = namedColors.find { case (k,v) => v == attr }
        entry match {
          case Some((k,v)) => new NamedColor(k)
          case None => throw new IllegalArgumentException(s"Unable to convert to a named color: ${attr}")
        }
      } else if (rgb.a == 0) {
        if (rgb.r==0&&rgb.g==0&&rgb.b==0) new NamedColor("transparent")
        else throw new IllegalArgumentException(f"Unable to convert to a named color: rgb(${rgb.r} ${rgb.b} ${rgb.g} / ${rgb.a}%.2f%%)")
      } else {
        throw new IllegalArgumentException(s"Unable to convert to a named color alpha must be 100")
      }
    }
  }

  def isNameValid( name: String ) = {
    val n = name.toLowerCase
    namedColors.contains(n) || n == "transparent" || n == "currentcolor"
  }

  val namedColors = Map(
    // CSS Level 1
    "black" -> "#000000",
    "silver" -> "#c0c0c0",
    "gray" -> "#808080",
    "white" -> "#ffffff",
    "maroon" -> "#800000",
    "red" -> "#ff0000",
    "purple" -> "#800080",
    "fuchsia" -> "#ff00ff",
    "green" -> "#008000",
    "lime" -> "#00ff00",
    "olive" -> "#808000",
    "yellow" -> "#ffff00",
    "navy" -> "#000080",
    "blue" -> "#0000ff",
    "teal" -> "#008080",
    "aqua" -> "#00ffff",
    // CSS Level 2 revision 1
    "orange" -> "#ffa500",
    // CSS Color Module Level 3
    "aliceblue" -> "#f0f8ff",
    "antiquewhite" -> "#faebd7",
    "aquamarine" -> "#7fffd4",
    "azure" -> "#f0ffff",
    "beige" -> "#f5f5dc",
    "bisque" -> "#ffe4c4",
    "blanchedalmond" -> "#ffebcd",
    "blueviolet" -> "#8a2be2",
    "brown" -> "#a52a2a",
    "burlywood" -> "#deb887",
    "cadetblue" -> "#5f9ea0",
    "chartreuse" -> "#7fff00",
    "chocolate" -> "#d2691e",
    "coral" -> "#ff7f50",
    "cornflowerblue" -> "#6495ed",
    "cornsilk" -> "#fff8dc",
    "crimson" -> "#dc143c",
    "cyan" -> "#00ffff",
    "darkblue" -> "#00008b",
    "darkcyan" -> "#008b8b",
    "darkgoldenrod" -> "#b8860b",
    "darkgray" -> "#a9a9a9",
    "darkgreen" -> "#006400",
    "darkgrey" -> "#a9a9a9",
    "darkkhaki" -> "#bdb76b",
    "darkmagenta" -> "#8b008b",
    "darkolivegreen" -> "#556b2f",
    "darkorange" -> "#ff8c00",
    "darkorchid" -> "#9932cc",
    "darkred" -> "#8b0000",
    "darksalmon" -> "#e9967a",
    "darkseagreen" -> "#8fbc8f",
    "darkslateblue" -> "#483d8b",
    "darkslategray" -> "#2f4f4f",
    "darkslategrey" -> "#2f4f4f",
    "darkturquoise" -> "#00ced1",
    "darkviolet" -> "#9400d3",
    "deeppink" -> "#ff1493",
    "deepskyblue" -> "#00bfff",
    "dimgray" -> "#696969",
    "dimgrey" -> "#696969",
    "dodgerblue" -> "#1e90ff",
    "firebrick" -> "#b22222",
    "floralwhite" -> "#fffaf0",
    "forestgreen" -> "#228b22",
    "gainsboro" -> "#dcdcdc",
    "ghostwhite" -> "#f8f8ff",
    "gold" -> "#ffd700",
    "goldenrod" -> "#daa520",
    "greenyellow" -> "#adff2f",
    "grey" -> "#808080",
    "honeydew" -> "#f0fff0",
    "hotpink" -> "#ff69b4",
    "indianred" -> "#cd5c5c",
    "indigo" -> "#4b0082",
    "ivory" -> "#fffff0",
    "khaki" -> "#f0e68c",
    "lavender" -> "#e6e6fa",
    "lavenderblush" -> "#fff0f5",
    "lawngreen" -> "#7cfc00",
    "lemonchiffon" -> "#fffacd",
    "lightblue" -> "#add8e6",
    "lightcoral" -> "#f08080",
    "lightcyan" -> "#e0ffff",
    "lightgoldenrodyellow" -> "#fafad2",
    "lightgray" -> "#d3d3d3",
    "lightgreen" -> "#90ee90",
    "lightgrey" -> "#d3d3d3",
    "lightpink" -> "#ffb6c1",
    "lightsalmon" -> "#ffa07a",
    "lightseagreen" -> "#20b2aa",
    "lightskyblue" -> "#87cefa",
    "lightslategray" -> "#778899",
    "lightslategrey" -> "#778899",
    "lightsteelblue" -> "#b0c4de",
    "lightyellow" -> "#ffffe0",
    "limegreen" -> "#32cd32",
    "linen" -> "#faf0e6",
    "magenta" -> "#ff00ff",
    "mediumaquamarine" -> "#66cdaa",
    "mediumblue" -> "#0000cd",
    "mediumorchid" -> "#ba55d3",
    "mediumpurple" -> "#9370db",
    "mediumseagreen" -> "#3cb371",
    "mediumslateblue" -> "#7b68ee",
    "mediumspringgreen" -> "#00fa9a",
    "mediumturquoise" -> "#48d1cc",
    "mediumvioletred" -> "#c71585",
    "midnightblue" -> "#191970",
    "mintcream" -> "#f5fffa",
    "mistyrose" -> "#ffe4e1",
    "moccasin" -> "#ffe4b5",
    "navajowhite" -> "#ffdead",
    "oldlace" -> "#fdf5e6",
    "olivedrab" -> "#6b8e23",
    "orangered" -> "#ff4500",
    "orchid" -> "#da70d6",
    "palegoldenrod" -> "#eee8aa",
    "palegreen" -> "#98fb98",
    "paleturquoise" -> "#afeeee",
    "palevioletred" -> "#db7093",
    "papayawhip" -> "#ffefd5",
    "peachpuff" -> "#ffdab9",
    "peru" -> "#cd853f",
    "pink" -> "#ffc0cb",
    "plum" -> "#dda0dd",
    "powderblue" -> "#b0e0e6",
    "rosybrown" -> "#bc8f8f",
    "royalblue" -> "#4169e1",
    "saddlebrown" -> "#8b4513",
    "salmon" -> "#fa8072",
    "sandybrown" -> "#f4a460",
    "seagreen" -> "#2e8b57",
    "seashell" -> "#fff5ee",
    "sienna" -> "#a0522d",
    "skyblue" -> "#87ceeb",
    "slateblue" -> "#6a5acd",
    "slategray" -> "#708090",
    "slategrey" -> "#708090",
    "snow" -> "#fffafa",
    "springgreen" -> "#00ff7f",
    "steelblue" -> "#4682b4",
    "tan" -> "#d2b48c",
    "thistle" -> "#d8bfd8",
    "tomato" -> "#ff6347",
    "turquoise" -> "#40e0d0",
    "violet" -> "#ee82ee",
    "wheat" -> "#f5deb3",
    "whitesmoke" -> "#f5f5f5",
    "yellowgreen" -> "#9acd32",
    // CSS Color Module Level 4
    "rebeccapurple" -> "#663399",

    // transparent keyword
    "transparent" -> "#00000000"
  )
}

/**
 * returns a Color object using RGB to construct it.
 *
 * @constructor
 * @param r  The red, in the range [0,255]
 * @param g  The green, in the range [0,255]
 * @param b  The blue, in the range [0,255]
 * @param a  The alpha as a percent.  0 - transparent and 100 full opacity
 */
case class RGBColor( r: Int, g: Int, b: Int, a: Double = 100 ) extends Color {
  def toAttrValue = f"""rgb($r,$g,$b${ColorInternal.aToString(a)})"""

  def toAttrValueHex = f"""#$r%02X$g%02X$b%02X${ColorInternal.aToHex(a)}"""

  def toRGBColor = this

  def toRGBPercentColor = {
    val rr = r/2.55
    val gg = g/2.55
    val bb = b/2.55
    new RGBPercentColor(rr,gg,bb,a)
  }

  def toHSLColor = {
    val rr = r/255.0
    val gg = g/255.0
    val bb = b/255.0
    HSLColor.rgbToHSLColor(rr, gg, bb, a)
  }

  def toNamedColor = NamedColor.toNamed(this)
}

/**
 * returns a Color object using RGB to construct it.
 *
 * @param r  The red as a percent, in the range [0,100]
 * @param g  The green as a percent, in the range [0,100]
 * @param b  The blue as a percent, in the range [0,100]
 * @param a  The alpha as a percent.  0 - transparent and 100 full opacity
 */
case class RGBPercentColor( r: Double, g: Double, b: Double, a: Double = 100 ) extends Color {
  def toAttrValue = f"""rgb($r%.2f%%,$g%.2f%%,$b%.2f%%${ColorInternal.aToString(a)})"""

  def toRGBPercentColor = this

  def toRGBColor = {
    val rr = r*2.55
    val gg = g*2.55
    val bb = b*2.55
    def round( v: Double ) = Math.round(v).toInt
    new RGBColor(round(rr),round(gg),round(bb),a)
  }

  def toHSLColor = {
    val rr = r/100
    val gg = g/100
    val bb = b/100
    HSLColor.rgbToHSLColor(rr, gg, bb, a)
  }

  def toNamedColor = NamedColor.toNamed(this)
}

/**
 * Converts an HSL color value to Color object. Conversion formula
 * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
 *
 * @constructor
 * @param hue  The hue, contained in the set [0, 360].  If not in range, modulo 360 is used.
 *           0 is red, 60 is yellow, 120 is green, 180 is cyan, 240 is blue, 300 is magenta
 * @param saturation  The saturation, contained in the set [0, 100].  If not in range, 0 or 100 is used.
 *           0 is gray (lack of color). 100 is full color.
 * @param lightness  The lightness, contained in the set [0, 100].  If not in range, 0 or 100 is used.
 *           0 is black, 100 is white.
 * @param a  The alpha.  0 - transparent and 100 full opacity
 */
case class HSLColor( hue: Double, saturation: Double, lightness: Double, a: Double = 100 ) extends Color {

  import HSLColor._

  def toAttrValue = f"""hsl($hue%.2f,$saturation%.2f%%,$lightness%.2f%%${ColorInternal.aToString(a)})"""

  def toRGBPercentColor = {
    val (r,g,b) = toRGB(hue,saturation,lightness)
    new RGBPercentColor( r*100.0, g*100.0, b*100.0, a )
  }

  def toRGBColor =  {
    val (r,g,b) = toRGB(hue,saturation,lightness)
    new RGBColor( Math.round(r*255).toInt, Math.round(g*255).toInt, Math.round(b*255).toInt, a )
  }

  def toHSLColor = this

  def toNamedColor = NamedColor.toNamed(this)
}

object HSLColor {

  def toRange( v: Double, min: Double, max: Double ) = {
    Math.max( min, Math.min( v, max ) )
  }

  def modulo( v: Double, m: Double ) = {
    val r = v%m;
    if (r<0) r+m
    else r
  }

  /**
   * @param rr the red - [0,1]
   * @param gg the green - [0,1]
   * @param bb the blue - [0,1]
   * @param a the alpha - [0,100]
   */
  def rgbToHSLColor( rr: Double, gg: Double, bb: Double, a: Double ) = {

    val max = Math.max(rr, Math.max(gg, bb))
    val min = Math.min(rr, Math.min(gg, bb))
    val c = max - min

    val hp = if (c == 0) {
      0.0
    } else if (max == rr) {
      (gg-bb)/c
    } else if (max == gg) {
      (bb-rr)/c+2
    } else if (max == bb) {
      (rr-gg)/c+4
    } else {
      0.0
    }
    val hpre = 60*hp
    val h = if (hpre<0) hpre+360 else hpre

    val l = (max+min)/2

    val spre = if (l == 1) 0
    else c/( 1 - Math.abs(2*l-1) )

    val s = if (spre.isNaN()) 0 else spre

    new HSLColor(h,s*100,l*100,a)
  }

  /**
   * @param hue  The hue, contained in the set [0, 360].  If not in range, modulo 360 is used.
   *           0 is red, 60 is yellow, 120 is green, 180 is cyan, 240 is blue, 300 is magenta
   * @param saturation  The saturation, contained in the set [0, 100].  If not in range, 0 or 100 is used.
   *           0 is gray (lack of color). 100 is full color.
   * @param lightness  The lightness, contained in the set [0, 100].  If not in range, 0 or 100 is used.
   *           0 is black, 100 is white.
   * @return tuple3 of doubles.  All values between [0,1].  Values are for red, green, blue
   */
  private def toRGB( hue: Double, saturation: Double, lightness: Double) = {
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
    ( (r1+m), (g1+m), (b1+m) )
  }

}
