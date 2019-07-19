package com.github.thebridsk.bridge.clientcommon.color

// addapted from https://www.w3.org/TR/css-color-4/

/**
 * @constructor
 * @param gray the amount of gray, 0 - black, 100 - white, numbers upto 400 allowed to represent bright white.
 * @param alpha the alpha channel value as a percent, 0 is transparent, 100 is opaque
 */

/**
 * @param gray 0 is black, 100 is white
 * @param alpha the alpha channel value as a percent, 0 is transparent, 100 is opaque
 */
case class Gray( gray: Double, alpha: Double = 100 ) extends Color {

  def toLab = Lab( gray, 0, 0, alpha )

  def toAttrValue = toRGBPercentColor.toAttrValue

  def toHSLColor = toRGBPercentColor.toHSLColor

  def toRGBColor = toRGBPercentColor.toRGBColor

  def toRGBPercentColor = {
    toLab.toXYZ.D50_to_D65.XYZ_to_lin_sRGB.gammaEncoding
  }

  def toNamedColor = toRGBColor.toNamedColor

}

/**
 * @constructor
 * @param cieLightness the amount of lightness, 0 - black, 100 - white, numbers upto 400 allowed to represent bright white.
 * @param a
 * @param b
 * @param alpha the alpha channel value as a percent, 0 is transparent, 100 is opaque
 *
 */
case class Lab( cieLightness: Double, a: Double, b: Double, alpha: Double = 100 ) extends Color {

  def toXYZ = {
    val f1 = (cieLightness+16)/116
    val f0 = a/500+f1
    val f2 = f1 - b/200

    import Lab._
    val xyz = Array(
        if (Math.pow(f0, 3) > e) Math.pow(f0, 3) else (116*f0-16)/k,
        if (cieLightness > k*e) Math.pow(f1,3) else cieLightness/k,
        if (Math.pow(f2, 3) > e) Math.pow(f2, 3) else (116*f2-16)/k
    ).zip(white).map { entry => entry._1*entry._2 }

    XYZ( xyz(0), xyz(1), xyz(2), alpha )
  }

  def toAttrValue = toRGBPercentColor.toAttrValue

  def toHSLColor = toRGBPercentColor.toHSLColor

  def toRGBColor = toRGBPercentColor.toRGBColor

  def toRGBPercentColor = toXYZ.D50_to_D65.XYZ_to_lin_sRGB.gammaEncoding

  def toNamedColor = toRGBColor.toNamedColor

}

object Lab {
  val k = 24389.0/27    // 29^3/3^3
  val e = 216.0/24389   // 6^3/29^3
  val white = Array(0.9642, 1.0000, 0.8249)   // D50 reference white
}


/**
 * @constructor
 * @param x
 * @param y
 * @param z
 * @param alpha the alpha channel value as a percent, 0 is transparent, 100 is opaque
 *
 */
case class XYZ( x: Double, y: Double, z: Double, alpha: Double = 100 ) extends Color {


  def D50_to_D65 = {
    // Bradford chromatic adaptation from D50 to D65
    val xyz = XYZ.MatrixD50ToD65.multiply(Array(x,y,z))

    XYZD65( xyz(0), xyz(1), xyz(2), alpha )
  }

  def toAttrValue = toRGBPercentColor.toAttrValue

  def toHSLColor = toRGBPercentColor.toHSLColor

  def toRGBColor = toRGBPercentColor.toRGBColor

  def toRGBPercentColor = D50_to_D65.XYZ_to_lin_sRGB.gammaEncoding

  def toNamedColor = toRGBColor.toNamedColor

}

/**
 * @constructor
 * @param x
 * @param y
 * @param z
 * @param alpha the alpha channel value as a percent, 0 is transparent, 100 is opaque
 *
 */
case class XYZD65( x: Double, y: Double, z: Double, alpha: Double = 100 ) extends Color {

  def XYZ_to_lin_sRGB = {
    // convert XYZ to linear-light sRGB
    val xyz = XYZ.MatrixD65ToLinsRGB.multiply(Array(x,y,z))

    SRGB( xyz(0), xyz(1), xyz(2), alpha )
  }

  def toAttrValue = toRGBPercentColor.toAttrValue

  def toHSLColor = toRGBPercentColor.toHSLColor

  def toRGBColor = toRGBPercentColor.toRGBColor

  def toRGBPercentColor = XYZ_to_lin_sRGB.gammaEncoding

  def toNamedColor = toRGBColor.toNamedColor

}

object XYZ {
  val MatrixD50ToD65 = Matrix(
      Array( 0.9555766, -0.0230393,  0.0631636),
      Array(-0.0282895,  1.0099416,  0.0210077),
      Array( 0.0122982, -0.0204830,  1.3299098)
      )

  val MatrixD65ToLinsRGB = Matrix(
      Array( 3.2404542, -1.5371385, -0.4985314),
      Array(-0.9692660,  1.8760108,  0.0415560),
      Array( 0.0556434, -0.2040259,  1.0572252)
      )
}

/**
 * @constructor
 * @param r  between 0 and 1
 * @param g  between 0 and 1
 * @param b  between 0 and 1
 * @param alpha the alpha channel value as a percent, 0 is transparent, 100 is opaque
 *
 */
case class SRGB( r: Double, g: Double, b: Double, alpha: Double = 100 ) {

  private def f( v: Double ) = {
      if (v > 0.0031308) {
        1.055 * Math.pow(v, 1/2.4) - 0.055;
      } else {
        12.92 * v;
      }
  }

  def gammaEncoding = {
    RGBPercentColor( f(r)*100, f(g)*100, f(b)*100, alpha )
  }

  def toAttrValue = toRGBPercentColor.toAttrValue

  def toHSLColor = toRGBPercentColor.toHSLColor

  def toRGBColor = toRGBPercentColor.toRGBColor

  def toRGBPercentColor = gammaEncoding

  def toNamedColor = toRGBColor.toNamedColor

}

case class Matrix( rows: Array[Double]* ) {

  /**
   * does Matrix*vector.
   * rows.length == vector.length must be true
   */
  def multiply( vector: Array[Double] ) = {
    rows.map { row =>
      row.zip(vector).foldLeft(0.0) { (ac,v) => ac+v._1*v._2 }
    }
  }
}
