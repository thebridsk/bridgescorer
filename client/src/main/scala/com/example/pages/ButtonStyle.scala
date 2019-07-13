package com.github.thebridsk.bridge.pages

import scala.scalajs.js

object ButtonStyle {
//    val playButton = cls("rootPlayButton
//                          baseAppButton100
//                          baseAppButton
//                          baseDefaultButton
//                          baseFontTextLarge")

  def apply() = {
    val p = js.Dynamic.literal()
//    val root = buttonStyle
//    p.updateDynamic("contained")(root)

    p.updateDynamic("contained")("baseMuiButton")

    p.asInstanceOf[js.Object]
  }

  def buttonStyle = {
    val root = js.Dynamic.literal()

    root.updateDynamic("margin")("6px")
    root.updateDynamic("whiteSpace")("nowrap")
    root.updateDynamic("padding")("3px 6px")
    root.updateDynamic("minWidth")("37px")
    root.updateDynamic("margin")("2px")

    root.updateDynamic("borderRadius")("20px")
    root.updateDynamic("backgroundColor")("rgb(210,210,210)")
    root.updateDynamic("borderStyle")("outset")
    root.updateDynamic("borderWidth")("2px")
    root.updateDynamic("userSelect")("none")
    root.updateDynamic("-webkitUserSelect")("none")
    root.updateDynamic("-mozUserSelect")("none")
    root.updateDynamic("-msUserSelect")("none")
    root.updateDynamic("fontSize")("x-large")
    root.updateDynamic("fontFamily")("Arial")
    root
  }

}
