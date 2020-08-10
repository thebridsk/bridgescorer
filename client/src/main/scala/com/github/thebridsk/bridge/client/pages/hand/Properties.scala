package com.github.thebridsk.bridge.client.pages.hand

import com.github.thebridsk.bridge.client.pages.Pixels

object Properties {

  lazy val defaultHandButtonFont: String = Pixels.getFont("DefaultHandButton")

  lazy val defaultHandButtonBorderRadius: Int =
    Pixels.getBorderRadius("DefaultHandButton")
  lazy val defaultHandButtonPaddingBorder: Int =
    Pixels.getPaddingBorder("DefaultHandButton")

  lazy val defaultHandVulPaddingBorder: Int =
    Pixels.getPaddingBorder("DefaultHandVul")

}
