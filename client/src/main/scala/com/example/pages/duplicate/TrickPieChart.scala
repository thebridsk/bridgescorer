package com.example.pages.duplicate

import japgolly.scalajs.react.vdom.html_<^._
import com.example.color.Color
import com.example.react.PieChart
import com.example.data.duplicate.stats.CounterStat
import com.example.react.PieChartTable.DataPieChart
import com.example.pages.BaseStyles.baseStyles
import com.example.react.PieChartTable

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

  /**
   * Returns an li element with a piechart made up of the colors followed by the title.
   * @param title the title of this entry
   * @param n the size of this entry in the piechart
   * @param total the total of all entries in piechart
   * @param colors the colors this entry covers in the piechart
   */
  def legendEntry(
      title: String,
      n: Int,
      total: Int,
      colors: Color*
  ) = {
    <.li(
      PieChart( 15, colors.map(c=>1.0).toList, colors.toList ),
      " ",
      title,
      f"${n} (${100.0*n/total}%.2f%%)"
    )
  }

  def tricksToTitle( tricks: Int ) = {
    if (tricks < 0) s"Down $tricks"
    else if (tricks == 0) "Made   :"
    else if (tricks == 10) "Passed:"
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

  /**
   * Returns a TagMod for this section in the legend.
   * The TagMod consists of of an li element and an optional ul element.
   * The ul element is only present if there are multiple entries in detail.
   * @param title the title of this entry
   * @param n the size of this entry in the piechart
   * @param total the total of all entries in piechart
   * @param detail the counters in order.  Must only contain counters of one type: made, down, or passed out
   */
  def legendSection(
      total: Int,
      detail: List[CounterStat],
  ) = {
    if (detail.isEmpty) {
      TagMod()
    } else if (detail.length == 1) {
      legendEntry( tricksToTitle(detail.head.tricks), detail.head.counter, total, colorMap(detail.head.tricks) )
    } else {
      val title = {
        val n = detail.head.tricks
        if (n < 0) "Down"
        else if (n == 10) "Passed out"
        else "Made"
      }
      val (subtotal, colors) = detail.map(_.counter).foldLeft(0, List[Color]()) { (ac,v) =>
        ( ac._1+v, colorMap(v)::ac._2 )
      }
      TagMod(
        legendEntry( title, subtotal, total, colors:_* ),
        <.ul(
          detail.map { cs =>
            legendEntry( tricksToTitle(cs.tricks), cs.counter, subtotal, colorMap(cs.tricks) )
          }.toTagMod
        )
      )
    }
  }

  /**
   * Returns the legend as a TagMod
   * @param histogram the counters in order.
   * @param title an optional legend title
   */
  def legend(
    histogram: List[CounterStat],
    title: Option[String] = None
  ) = {
    if (histogram.isEmpty) TagMod()
    else {
      // Map, index is -1 for down, 1 for made, 10 for passed
      // index determines the order they are shown in legend.
      val bytype = histogram.groupBy { cs=>
        if (cs.tricks<0) -1
        else if (cs.tricks==10) 10
        else 1
      }.toList.sortBy(_._1).map( _._2 )
      val total = histogram.foldLeft(0) { (ac,v) => ac+v.counter }
      <.ul(
        title.whenDefined( t => <.li(t) ),
        bytype.map { list =>
          legendSection(total, list)
        }.toTagMod
      )
    }
  }

  /**
   * Returns the legend as a TagMod
   * @param title the title for the tooltip
   * @param histogram the counters in order.
   * @param piechart the piechart to create the legend for
   * @param size the size of the piechart in the tooltip
   * @param legendtitle an optional legend title
   */
  def Cell(
    histogram: List[CounterStat],
    piechart: DataPieChart,
    size: Int,
    title: Option[String],
    legendtitle: Option[String] = None
  ) = {
    TagMod(
      title.whenDefined( t => <.div( baseStyles.tooltipTitle, t ) ),
      <.div(
        baseStyles.tooltipBody,
        PieChartTable.item(piechart.withSize(size)),
        <.div(legend(histogram,legendtitle))
      )
    )
  }
}
