package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip.IntLegendUtil
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip

/**
  * A component that displays a pie chart with up to three slices, and a tooltip.
  *
  * Slices:
  * - green, for good results
  * - red, for bad results
  * - gray, for neutral results
  *
  * The tooltip has an optional title, a bigger version of the piechart,
  * and a legend, with an optional legend title.
  *
  * Usage:
  * {{{
  * ComparisonPieChart(
  *   good = 2,
  *   bad = 1,
  *   neutral = 3,
  *   title = Some("good vs bad"),
  *   legendtitle = Left(true),
  *   size = 100,
  *   sizeInLegend = 200,
  *   minSize = 100
  * )
  * }}}
  *
  * See the [[apply]] method for a description of the arguments.
  *
  */
object ComparisonPieChart {

  val ColorGood: Color = Color.hsl(120, 100, 50.0) // green
  val ColorBad: Color = Color.hsl(0, 100, 50.0) // red
  val ColorNeutral: Color = Color.grayscale(50) // gray

  private object TrickLegendUtil extends IntLegendUtil[Color] {

    def nameToTitle(name: Color): String = {
      if (name eq ColorGood) "Good"
      else if (name eq ColorBad) "Bad"
      else if (name eq ColorNeutral) "Neutral"
      else "Unknown"
    }

    def colorMap(name: Color) = name
  }

  /**
    * Instantiate the component.
    *
    * @param good the number of good results.
    * @param bad the number of bad results.
    * @param neutral the number of neutral results.
    * @param title an optional title shown in the tooltip.
    * @param legendtitle an either legend title.
    *                    If Left(true), then "Total: <n>" is used.
    *                    If Left(false) then no title will be used.
    *                    If Right(title) then title will be used.
    * @param size      The size of the piechart on the page, in px.
    * @param sizeInLegend the size of the piechart in the legend in the tooltip, in px.
    * @param minSize   The minimum height of the element containing the pie chart, in px.
    *
    * @return the unmounted react component
    */
  def apply(
      good: Int,
      bad: Int,
      neutral: Int,
      title: Option[TagMod],
      legendtitle: Either[Boolean, TagMod],
      size: Int,
      sizeInLegend: Int,
      minSize: Int
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val bytype: List[(String, List[(Color, Int)])] = List(
      ("Good", List((ColorGood, good))),
      ("Neutral", List((ColorNeutral, neutral))),
      ("Bad", List((ColorBad, bad)))
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
}
