package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip.IntLegendUtil
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip
import com.github.thebridsk.bridge.clientcommon.react.ColorBar

/**
  * A component that displays a piechart for types of contract with a tooltip.
  *
  * Slices:
  * - partial
  * - game
  * - doubled to game
  * - slam
  * - grand slam
  *
  * The tooltip has an optional title, a bigger version of the piechart,
  * and a legend, with an optional legend title.
  *
  * Usage:
  * {{{
  * ContractTypePieChart(
  *   partial = 10,
  *   game = 7,
  *   slam = 1,
  *   grandslam = 0,
  *   passed = 1,
  *   title = Some("player"),
  *   legendtitle = Left(true),
  *   size = 100,
  *   sizeInLegend = 200,
  *   minSize = 100,
  *   doubledToGame = 1
  * )
  * }}}
  *
  * @see See [[apply]] method for a description of the arguments.
  */
object ContractTypePieChart {

  val ColorTypePartial: Color = Color.hsl(60, 100, 50.0) // yellow
  val ColorTypeGame: Color = Color.hsl(30, 100, 50.0) // orange
  val ColorTypeDoubledToGame: Color = Color.hsl(45, 100, 50.0) // orange
  val ColorTypeSlam: Color = Color.hsl(300, 100, 50.0) // purple
  val ColorTypeGrandSlam = Color.Cyan
  val ColorTypePassed = TrickPieChart.colorTypePassed

  private object TrickLegendUtil extends IntLegendUtil[Color] {

    def nameToTitle(name: Color): String = {
      if (name eq ColorTypePartial) "Partial"
      else if (name eq ColorTypeGame) "Game"
      else if (name eq ColorTypeDoubledToGame) "Doubled To Game"
      else if (name eq ColorTypeSlam) "Slam"
      else if (name eq ColorTypeGrandSlam) "Grand Slam"
      else "Passed Out"
    }

    def colorMap(name: Color) = name
  }

  /**
    * Instantiate the component
    *
    * @param partial number of partial contracts
    * @param game number of game contracts
    * @param slam number of bid slams
    * @param grandslam number of bid grand slams
    * @param passed number of hands that were passed out
    * @param title an optional title shown in the tooltip.
    * @param legendtitle an either legend title.
    *                    If Left(true), then "Total: <n>" is used.
    *                    If Left(false) then no title will be used.
    *                    If Right(title) then title will be used.
    * @param size      The size of the piechart on the page, in px.
    * @param sizeInLegend the size of the piechart in the legend in the tooltip, in px.
    * @param minSize   The minimum height of the element containing the pie chart, in px.
    * @param doubledToGame partial contracts that were doubled to game level.
    *                      This includes doubled and redoubled contracts.
    *
    * @return the unmounted react component.
    */
  def apply(
      partial: Int,
      game: Int,
      slam: Int,
      grandslam: Int,
      passed: Int,
      title: Option[TagMod],
      legendtitle: Either[Boolean, TagMod],
      size: Int,
      sizeInLegend: Int,
      minSize: Int,
      doubledToGame: Int = 0
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    val bytype: List[(String, List[(Color, Int)])] = List(
      ("Passed out", List((ColorTypePassed, passed))),
      ("Partial", List((ColorTypePartial, partial))),
      ("Doubled To Game", List((ColorTypeDoubledToGame, doubledToGame))),
      ("Game", List((ColorTypeGame, game))),
      ("Slam", List((ColorTypeSlam, slam))),
      ("Grand Slam", List((ColorTypeGrandSlam, grandslam)))
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

  /**
    *
    * @param withDoubled - if true, included doubled to game contracts.
    *
    * @return a colorbar that has tool tips to identify the type of contract.
    */
  def description(withDoubled: Boolean = false): TagMod = {
    val cs =
      ColorTypePassed :: ColorTypePartial :: ColorTypeDoubledToGame :: ColorTypeGame :: ColorTypeSlam :: ColorTypeGrandSlam :: Nil
    val colors =
      if (withDoubled) cs
      else cs.filter(c => c != ColorTypeDoubledToGame)
    TagMod(
      ColorBar.simple(
        colors,
        Some(colors.map(c => TagMod(TrickLegendUtil.nameToTitle(c))))
      )
    )
  }
}
