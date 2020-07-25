package com.github.thebridsk.bridge.client.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.data.duplicate.stats.CounterStat
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip.IntLegendUtil
import com.github.thebridsk.bridge.clientcommon.react.PieChartWithTooltip
import com.github.thebridsk.bridge.clientcommon.react.ColorBar
import com.github.thebridsk.bridge.clientcommon.react.Utils._

object TrickPieChart {

  val colorTypePassed = Color.Blue

  val allMadeColors = for (
                         hue <- 120::150::180::Nil;
                         lightness <- 75::50::25::Nil
                      ) yield {
                        Color.hsl(hue,100,lightness)
                      }

  val allDownColors = for (
                         hue <- 0::330::30::300::60::Nil;
                         lightness <- 25::50::75::Nil
                      ) yield {
                        Color.hsl(hue,100,lightness)
                      }

  object TrickLegendUtil extends IntLegendUtil[Int] {

    def nameToTitle( tricks: Int ) = {
      if (tricks < 0) s"Down $tricks"
      else if (tricks == 0) "Made   "
      else if (tricks == 10) "Passed"
      else s"Made +$tricks"
    }

    /**
     * returns the color based on the number of tricks.
     * @param tricks the number of tricks relative to contract
     *        <0 down contract, number of tricks down
     *        >=0 made contract, number of tricks made
     *        10 - passed out
     */
    def colorMap( i: Int ) = {
      if (i == 10) colorTypePassed
      else if (i < 0) allDownColors( -i-1 )
      else allMadeColors( i )
    }
  }


  /**
   * @param histogram
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
      histogram: List[CounterStat],
      title: Option[TagMod],
      legendtitle: Either[Boolean,TagMod],
      size: Int,
      sizeInLegend: Int,
      minSize: Int
  ) = {
    val bytype = histogram.groupBy { cs =>
      if (cs.tricks < 0) 2
      else if (cs.tricks == 10) 10
      else 1
    }.toList.sortBy(_._1)map { entry =>
      val (ty, list) = entry
      val stype = if (ty == 2) "Down"
                  else if (ty == 1) "Made"
                  else "Passed"
      val l = list.sortBy(_.tricks).map(cs => (cs.tricks,cs.counter))
      (stype, if (ty == 2) l.reverse else l)
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
   * @param maxDown a negative number indicating the maximum down result
   * @param maxMade a non-negative number indicating the maximum made result
   * @param nopassedout
   */
  def description( maxDown: Int, maxMade: Int, nopassedout: Boolean = false ) = {
    val p = if (nopassedout) (maxDown to maxMade).toList
            else (maxDown to maxMade).toList:::List( 10 )
    val colors = p.map( i => TrickLegendUtil.colorMap(i))
    val titles = p.map( i=>TagMod(TrickLegendUtil.nameToTitle(i)))
    TagMod(
      "The color indicates the result of the contract.",
      <.br,
      "Red indicates a down, green a made contract.  ",
      nopassedout ?= ", and blue a passed out contract.",
      ColorBar.simple( colors, Some( titles) )
    )
  }
}
