package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.clientcommon.react.PieChart
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip.IntLegendUtil
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip

object ComparisonPieChart {

  val ColorGood: Color = Color.hsl(120, 100, 50.0) // green
  val ColorBad: Color = Color.hsl(0, 100, 50.0) // red
  val ColorNeutral: Color = Color.grayscale(50) // gray

  object TrickLegendUtil extends IntLegendUtil[Color] {

    def nameToTitle(name: Color): String = {
      if (name eq ColorGood) "Good"
      else if (name eq ColorBad) "Bad"
      else if (name eq ColorNeutral) "Neutral"
      else "Unknown"
    }

    def colorMap(name: Color) = name
  }

  /**
    * @param good
    * @param bad
    * @param neutral
    * @param title
    * @param legendtitle an either legend title.
    *                    If Left(true), then "Total: <n>" is used.
    *                    If Left(false) then no title will be used.
    *                    If Right(title) then title will be used.
    * @param size
    * @param sizeInLegend
    * @param minSize
    * @param doubledToGame partial contracts that were doubled to game level
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

  def description: TagMod = {
    TagMod(
      "The ",
      <.b("Aggressive"),
      " and ",
      <.b("Passive"),
      " columns are when the same side is declarer at both tables.",
      <.br,
      "The ",
      <.b("Competitive Aggressive"),
      " and ",
      <.b("Competitive Passive"),
      " columns",
      <.br,
      " are when the contracts are declared from different sides at the tables.",
      <.br,
      "The Aggressive is the higher contract, Passive is the lower contract",
      <.br,
      "Good, ",
      PieChart(
        15,
        1.0 :: Nil,
        Some(ColorGood :: Nil),
        attrs = Some(^.display := "inline-block")
      ),
      <.ul(
        <.li(
          "team with higher contract, aggressive, was successful.  example: made game over partial"
        ),
        <.li(
          "team with lower contract, passive, was successful.  Example: made partial over down game"
        ),
        <.li(
          "the team with higher contract, competitive aggressive, was successful.  Example: 5C down 1 vs 4S made"
        ),
        <.li(
          "the team with lower contract, competitive passive, was successful.  Example: 5C* vul down 3 vs 4S made "
        )
      ),
      <.br,
      "Bad, ",
      PieChart(
        15,
        1.0 :: Nil,
        Some(ColorBad :: Nil),
        attrs = Some(^.display := "inline-block")
      ),
      <.ul(
        <.li(
          "team with lower contract, passive, failed.  example: made game under partial"
        ),
        <.li(
          "team with higher contract, aggressive, failed.  Example: down game under made partial"
        ),
        <.li(
          "the team with lower contract, competitive passive, failed.  Example: 5C down 1 vs 4S made"
        ),
        <.li(
          "the team with higher contract, competitive aggressive, failed.  Example: 5C* vul down 3 vs 4S made "
        )
      ),
      <.br,
      "Neutral, ",
      PieChart(
        15,
        1.0 :: Nil,
        Some(ColorNeutral :: Nil),
        attrs = Some(^.display := "inline-block")
      ),
      <.ul(
        <.li("no difference, Example: both contracts were partial"),
        <.li(
          "contracts were played from different sides ending with same score, 1N made 3, 3C down 2"
        )
      )
    )
  }
}
