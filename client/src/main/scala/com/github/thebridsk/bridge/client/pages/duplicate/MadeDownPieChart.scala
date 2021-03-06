package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.clientcommon.react.PieChart
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip.IntLegendUtil
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip
import com.github.thebridsk.color.RGBColor

object MadeDownPieChart {

  val ColorAllowedMade: RGBColor = Color.rgb(164, 0, 0)
  val ColorTookDown: RGBColor = Color.rgb(0, 164, 0)
  val ColorDown: RGBColor = Color.rgb(255, 0, 0)
  val ColorMade: RGBColor = Color.rgb(0, 255, 0)
  val ColorPassedOut = TrickPieChart.colorTypePassed

  object TrickLegendUtil extends IntLegendUtil[Color] {

    def nameToTitle(name: Color): String = {
      if (name eq ColorAllowedMade) "Allowed made"
      else if (name eq ColorTookDown) "Took down"
      else if (name eq ColorDown) "Down"
      else if (name eq ColorMade) "Made"
      else "Passed out"
    }

    def colorMap(name: Color) = name
  }

  def apply(
      made: Int,
      down: Int,
      allowedMade: Int,
      tookDown: Int,
      passed: Int,
      title: Option[TagMod],
      legendtitle: Either[Boolean, TagMod],
      size: Int,
      sizeInLegend: Int,
      minSize: Int
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val bytype: List[(String, List[(Color, Int)])] = List(
      ("Declarer", List((ColorMade, made), (ColorDown, down))),
      (
        "Defender",
        List((ColorAllowedMade, allowedMade), (ColorTookDown, tookDown))
      ),
      ("Passed out", List((ColorPassedOut, passed)))
    ).flatMap { entry =>
      val (name, list) = entry
      val l = list.filter(_._2 != 0)
      if (l.isEmpty) Nil
      else (name, l) :: Nil
    }

    PieChartWithTooltip(
      histogram = bytype,
      title = title,
      legendtitle = legendtitle,
      util = TrickLegendUtil,
      size = size,
      sizeInLegend = sizeInLegend,
      minSize = minSize
    )

  }

  def description: TagMod =
    TagMod(
      "The size of the circle is proportional to the number of hands played by the pair/player. ",
      <.br,
      "Green, ",
      PieChart(
        15,
        1.0 :: Nil,
        Some(MadeDownPieChart.ColorMade :: Nil),
        attrs = Some(^.display := "inline-block")
      ),
      ", is contract made as declarer, red, ",
      PieChart(
        15,
        1.0 :: Nil,
        Some(MadeDownPieChart.ColorDown :: Nil),
        attrs = Some(^.display := "inline-block")
      ),
      ", is down as declarer.",
      <.br,
      "Dark green, ",
      PieChart(
        15,
        1.0 :: Nil,
        Some(MadeDownPieChart.ColorTookDown :: Nil),
        attrs = Some(^.display := "inline-block")
      ),
      ", is took down as defender, dark red, ",
      PieChart(
        15,
        1.0 :: Nil,
        Some(MadeDownPieChart.ColorAllowedMade :: Nil),
        attrs = Some(^.display := "inline-block")
      ),
      ", is allowed made as defender.",
      <.br,
      "Blue, ",
      PieChart(
        15,
        1.0 :: Nil,
        Some(MadeDownPieChart.ColorPassedOut :: Nil),
        attrs = Some(^.display := "inline-block")
      ),
      ", is passed out hands."
    )
}
