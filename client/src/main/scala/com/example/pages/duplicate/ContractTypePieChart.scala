package com.example.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import com.example.color.Color
import com.example.react.PieChart
import com.example.data.duplicate.stats.CounterStat
import com.example.pages.BaseStyles.baseStyles
import com.example.react.PieChartWithTooltip.IntLegendUtil
import com.example.react.PieChartWithTooltip
import com.example.react.ColorBar

object ContractTypePieChart {

  val ColorTypePartial: Color = Color.hsl( 60, 100, 50.0 )  // yellow
  val ColorTypeGame: Color = Color.hsl( 30, 100, 50.0 )  // orange
  val ColorTypeSlam: Color = Color.hsl( 300, 100, 50.0 ) // purple
  val ColorTypeGrandSlam = Color.Cyan
  val ColorTypePassed = TrickPieChart.colorTypePassed

  object TrickLegendUtil extends IntLegendUtil[Color] {

    def nameToTitle( name: Color ) = {
      if (name eq ColorTypePartial)        "Partial"
      else if (name eq ColorTypeGame)      "Game"
      else if (name eq ColorTypeSlam)      "Slam"
      else if (name eq ColorTypeGrandSlam) "Grand Slam"
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
      minSize: Int
  ) = {
    val bytype: List[(String,List[(Color,Int)])] = List(
          ("Passed out", List( (ColorTypePassed,passed) ) ),
          ("Partial", List( (ColorTypePartial,partial) ) ),
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

  def description = {
    val colors = ColorTypePassed::ColorTypePartial::ColorTypeGame::ColorTypeSlam::ColorTypeGrandSlam::Nil
    TagMod(
      ColorBar.simple( colors, Some(colors.map( c => TagMod( TrickLegendUtil.nameToTitle(c) ) )) )
    )
  }
}
