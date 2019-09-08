package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.clientcommon.react.PieChart
import com.github.thebridsk.bridge.data.duplicate.stats.CounterStat
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles.baseStyles
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip.IntLegendUtil
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip
import com.github.thebridsk.bridge.clientcommon.react.ColorBar

object ContractTypePieChart {

  val ColorTypePartial: Color = Color.hsl( 60, 100, 50.0 )  // yellow
  val ColorTypeGame: Color = Color.hsl( 30, 100, 50.0 )  // orange
  val ColorTypeDoubledToGame: Color = Color.hsl( 45, 100, 50.0 )  // orange
  val ColorTypeSlam: Color = Color.hsl( 300, 100, 50.0 ) // purple
  val ColorTypeGrandSlam = Color.Cyan
  val ColorTypePassed = TrickPieChart.colorTypePassed

  object TrickLegendUtil extends IntLegendUtil[Color] {

    def nameToTitle( name: Color ) = {
      if (name eq ColorTypePartial)             "Partial"
      else if (name eq ColorTypeGame)           "Game"
      else if (name eq ColorTypeDoubledToGame)  "Doubled To Game"
      else if (name eq ColorTypeSlam)           "Slam"
      else if (name eq ColorTypeGrandSlam)      "Grand Slam"
      else "Passed Out"
    }

    def colorMap( name: Color ) = name
  }

  /**
   *
   * @param partial
   * @param game
   * @param slam
   * @param grandslam
   * @param passed
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
      partial: Int,
      game: Int,
      slam: Int,
      grandslam: Int,
      passed: Int,
      title: Option[TagMod],
      legendtitle: Either[Boolean,TagMod],
      size: Int,
      sizeInLegend: Int,
      minSize: Int,
      doubledToGame: Int = 0
  ) = {
    val bytype: List[(String,List[(Color,Int)])] = List(
          ("Passed out", List( (ColorTypePassed,passed) ) ),
          ("Partial", List( (ColorTypePartial,partial) ) ),
          ("Doubled To Game", List( (ColorTypeDoubledToGame,doubledToGame) ) ),
          ("Game", List( (ColorTypeGame,game) ) ),
          ("Slam", List( (ColorTypeSlam,slam) ) ),
          ("Grand Slam", List( (ColorTypeGrandSlam,grandslam) ) )
        ).flatMap { entry =>
          val (name, list) = entry
          val l = list.filter(_._2 != 0)
          if (l.isEmpty) Nil
          else (name,l)::Nil
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

  def description( withDoubled: Boolean = false ) = {
    val cs = ColorTypePassed::ColorTypePartial::ColorTypeDoubledToGame::ColorTypeGame::ColorTypeSlam::ColorTypeGrandSlam::Nil
    val colors = if (withDoubled) cs
    else cs.filter( c => c != ColorTypeDoubledToGame )
    TagMod(
      ColorBar.simple( colors, Some(colors.map( c => TagMod( TrickLegendUtil.nameToTitle(c) ) )) )
    )
  }
}
