package com.github.thebridsk.bridge.data.util

case class ColorTheme( theme: String ) {

  def isDark = theme == ColorTheme.valDark
  def isLight = theme == ColorTheme.valLight
}

object ColorTheme {

  val valDark = "dark"
  val valLight = "light"

  val dark = ColorTheme(valDark)
  val light = ColorTheme(valLight)
}