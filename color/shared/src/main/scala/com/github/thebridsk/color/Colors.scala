package com.github.thebridsk.color

object Colors {

  /**
    * returns the colors.  minLightness will be used, maxLightness will not.
    * @param hue the hue [0 - 360]
    * @param minLightness the minimum lightness [0 - 100]
    * @param n the number of boxes for hue
    * @param darkToLight left boxes should be dark to light on right if true.  Default is true.
    * @param maxLightness the maximum lightness [0 - 100], default is 100
    * @param saturation the saturation value to use [0 - 100], default is 100
    * @return the colors.
    */
  def colorsExcludeEnd(
      hue: Double,
      minLightness: Double,
      n: Int,
      darkToLight1: Boolean = true,
      maxLightness: Double = 100.0,
      saturation: Double = 100.0
  ): List[HSLColor] = {
    if (n == 0) Nil
    else if (n == 1) Color.hsl(hue, saturation, 50.0) :: Nil
    else {
      val step = (maxLightness - minLightness) / n

      def loopUntil[T](start: Double, stop: Double, step: Double)(
          f: Double => T
      ): List[T] = {
        if (start >= stop) Nil
        else f(start) :: loopUntil(start + step, stop, step)(f)
      }

      val cols = loopUntil(minLightness, maxLightness, step) { l =>
        Color.hsl(hue, saturation, l.toDouble)
      }
      if (darkToLight1) cols
      else cols.reverse
    }
  }

  /**
    * Returns the colors from color1 to color2, interpolating on the hsl values.
    * The specified colors are converted to HSLColor.
    * @param color1
    * @param color2
    * @param n the number of colors to return
    */
  def colors(
      color1: Color,
      color2: Color,
      n: Int
  ): List[HSLColor] = {
    colorsHSL(color1.toHSLColor, color2.toHSLColor, n)
  }

  /**
    * Returns the colors from color1 to color2, interpolating on the rgb or hsl values.
    * The specified colors are converted to HSLColor or RGBPercentColor.
    * @param color1
    * @param color2
    * @param n the number of colors to return
    * @param byRGB if true rgb, false hsl
    */
  def colors(
      color1: Color,
      color2: Color,
      n: Int,
      byRGB: Boolean
  ): List[Color] = {
    if (byRGB) colorsRGB(color1.toRGBPercentColor, color2.toRGBPercentColor, n)
    else colorsHSL(color1.toHSLColor, color2.toHSLColor, n)
  }

  /**
    * Returns the colors from color1 to color2, interpolating on the rgb channels.
    * @param color1
    * @param color2
    * @param n the number of colors to return
    */
  def colorsRGB(
      color1: RGBPercentColor,
      color2: RGBPercentColor,
      n: Int
  ): List[RGBPercentColor] = {
    if (n == 1)
      Color.rgbPercent(
        (color1.r + color2.r) / 2,
        (color1.g + color2.g) / 2,
        (color1.b + color2.b) / 2,
        (color1.a + color2.a) / 2
      ) :: Nil
    else if (n == 2) color1 :: color2 :: Nil
    else {
      val rstep = (color1.r - color2.r) / (n - 1)
      val gstep = (color1.g - color2.g) / (n - 1)
      val bstep = (color1.b - color2.b) / (n - 1)
      val astep = (color1.a - color2.a) / (n - 1)

      (1 until n).foldLeft(color2 :: Nil) { (ac, v) =>
        val c = ac.head
        Color.rgbPercent(c.r + rstep, c.g + gstep, c.b + bstep, c.a + astep) :: ac
      }
    }
  }

  /**
    * Returns the colors from color1 to color2, interpolating on the hsl values.
    * @param color1
    * @param color2
    * @param n the number of colors to return
    */
  def colorsHSL(
      color1: HSLColor,
      color2: HSLColor,
      n: Int
  ): List[HSLColor] = {
    if (n == 1)
      Color.hsl(
        (color1.hue + color2.hue) / 2,
        (color1.saturation + color2.saturation) / 2,
        (color1.lightness + color2.lightness) / 2,
        (color1.a + color2.a) / 2
      ) :: Nil
    else if (n == 2) color1 :: color2 :: Nil
    else {
      val hstep = (color1.hue - color2.hue) / (n - 1)
      val sstep = (color1.saturation - color2.saturation) / (n - 1)
      val lstep = (color1.lightness - color2.lightness) / (n - 1)
      val astep = (color1.a - color2.a) / (n - 1)

      (1 until n).foldLeft(color2 :: Nil) { (ac, v) =>
        val c = ac.head
        Color.hsl(
          c.hue + hstep,
          c.saturation + sstep,
          c.lightness + lstep,
          c.a + astep
        ) :: ac
      }
    }
  }

  /**
    * returns the colors
    * @param hue the hue
    * @param minLightness the minimum lightness [0 - 100]
    * @param n the number of boxes for hue
    * @param darkToLight left boxes should be dark to light on right if true.  Default is true.
    * @param maxLightness the maximum lightness [0 - 100], default is 100
    * @param saturation the saturation value to use [0 - 100], default is 100
    * @return the colors.
    */
  def colors(
      hue: Double,
      minLightness: Double,
      n: Int,
      darkToLight1: Boolean = true,
      maxLightness: Double = 100.0,
      saturation: Double = 100.0
  ): List[HSLColor] = {
    if (n == 0) Nil
    else if (n == 1) Color.hsl(hue, saturation, 50.0) :: Nil
    else {
      val step = (maxLightness - minLightness) / n

      def loopTo[T](start: Double, stop: Double, step: Double)(
          f: Double => T
      ): List[T] = {
        if (start > stop) Nil
        else f(start) :: loopTo(start + step, stop, step)(f)
      }

      val cols = loopTo(minLightness, maxLightness, step) { l =>
        Color.hsl(hue, saturation, l.toDouble)
      }
      if (darkToLight1) cols
      else cols.reverse
    }
  }

}
