package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import scala.language.postfixOps
import com.github.thebridsk.bridge.clientcommon.color.Color

object PieChartWithTooltip {

  /**
   * @param N the type of the name that identifies a slice in the piechart.
   */
  trait LegendUtil[N,V] {

    /**
     * returns the title for the slice.  This is used in the legend.
     * @param name the name of the slice
     */
    def nameToTitle( name: N ): String


    /**
     * returns the color for the slice
     * @param name the name of the slice
     */
    def colorMap( name: N ): Color

    /**
     * format the value for display
     * @param value
     */
    def formatValue( value: V ): String

    /**
     * the zero value for V
     */
    def zeroValue: V

    /**
     * add two values of type V.
     */
    def addValue( l: V, r: V ): V

    /**
     * divide values of type V
     */
    def divideValue( n: V, d: V ): Double

    def valueToDouble( v: V ): Double

    /**
     * Returns an li element with a piechart made up of the colors followed by the title.
     * @param title the title of this entry
     * @param n the size of this entry in the piechart
     * @param total the total of all entries in this section of the piechart
     * @param total2 an optional overall total in the piechart
     * @param colors the colors this entry covers this section of the piechart
     */
    def legendEntry(
        title: String,
        n: V,
        total: V,
        total2: Option[V],
        colors: Color*
    ) = {
      val l2 = total2.map( t => f", ${100.0*divideValue(n,t)}%.2f%%" ).getOrElse("")
      val l = f": ${formatValue(n)} (${100.0*divideValue(n,total)}%.2f%%${l2})"
      <.li(
        PieChart( 15, colors.map(c=>1.0).toList, colors.toList ),
        " ",
        title,
        l
      )
    }

    /**
     * Returns a TagMod for this section in the legend.
     * The TagMod consists of of an li element and an optional ul element.
     * The ul element is only present if there are multiple entries in detail.
     * @param title the title of this section
     * @param total the total of all entries in piechart
     * @param detail the counters in order.
     */
    def legendSection(
        title: String,
        total: V,
        detail: List[(N,V)],
    ) = {
      if (detail.isEmpty) {
        TagMod()
      } else if (detail.length == 1) {
        legendEntry( nameToTitle(detail.head._1), detail.head._2, total, None, colorMap(detail.head._1) )
      } else {
        val (subtotal, colors) = detail.foldLeft(zeroValue, List[Color]()) { (ac,v) =>
          ( addValue(ac._1,v._2), colorMap(v._1)::ac._2 )
        }
        TagMod(
          legendEntry( title, subtotal, total, None, colors:_* ),
          <.ul(
            detail.map { cs =>
              legendEntry( nameToTitle(cs._1), cs._2, subtotal, Some(total), colorMap(cs._1) )
            }.toTagMod
          )
        )
      }
    }

    /**
     * Returns the legend as a TagMod
     * @param histogram the counters in order organized in sections
     * @param title an optional legend title.  If None, then "Total: <n>" is used.
     */
    def legend(
      histogram: List[(String,List[(N,V)])],
      title: Either[Boolean,TagMod]
    ) = {
      if (histogram.isEmpty) TagMod()
      else {
        val total = histogram.flatMap( l => l._2).foldLeft(zeroValue) { (ac,v) => addValue(ac,v._2) }
        <.ul(
          title match {
            case Left(false) => TagMod.empty
            case Left(true) => <.li( s"Total: ${formatValue(total)}" )
            case Right(t) => <.li(t)
          },
          histogram.map { entry =>
            val (sectionTitle, list) = entry
            legendSection(sectionTitle, total, list)
          }.toTagMod
        )
      }
    }
  }

  /**
   * Legend for Int values for pie slice sizes
   */
  trait IntLegendUtil[N] extends LegendUtil[N,Int] {

    def formatValue( value: Int ): String = value.toString()

    def zeroValue = 0

    def addValue( l: Int, r: Int ) = l+r

    def divideValue( n: Int, d: Int ) = n.toDouble/d

    def valueToDouble( v: Int ) = v.toDouble

  }

  /**
   * Legend for Double values for pie slice sizes
   */
  trait DoubleLegendUtil[N] extends LegendUtil[N,Double] {

    def formatValue( value: Double ): String = value.toString()

    def zeroValue = 0.0

    def addValue( l: Double, r: Double ) = l+r

    def divideValue( n: Double, d: Double ) = n/d

    def valueToDouble( v: Double ) = v

  }

  case class Props[N,V](
      histogram: List[(String,List[(N,V)])],
      title: Option[TagMod],
      legendtitle: Either[Boolean,TagMod],
      util: LegendUtil[N,V],
      size: Int,
      sizeInLegend: Int,
      minSize: Int
  )

  /**
   * Component that shows a pie chart.  When the mouse goes over the piechart, a tooltip is shown
   * with the piechart in a standard size and a legend.
   *
   * @param histogram a list of histogram.  The entry is a section that is a tuple2 with a name and histogram.
   *                  The legend will have totals for each section, and a total for the entire histogram.
   * @param title     The title that is shown in the tooltip that also shows the legend.
   * @param legendtitle an either legend title.
   *                    If Left(true), then "Total: <n>" is used.
   *                    If Left(false) then no title will be used.
   *                    If Right(title) then title will be used.
   * @param util      a LegendUtil object used to obtained the TagMod values that are put into the pie chart.
   * @param size      The size of the piechart on the page
   * @param sizeInLegend the size of the piechart in the legend in the tooltip
   * @param minSize   The minimum height of the element containing the pie chart
   *
   * @param N the type for the name.  This is the type of the first entry in the tuple2 of the histogram.
   * @param V the type for the value.
   */
  def apply[N,V](
      histogram: List[(String,List[(N,V)])],
      title: Option[TagMod],
      legendtitle: Either[Boolean,TagMod],
      util: LegendUtil[N,V],
      size: Int,
      sizeInLegend: Int,
      minSize: Int
  ) = PieChartWithTooltipInternal.component(Props(histogram,title,legendtitle,util.asInstanceOf[LegendUtil[Any,Any]],size,sizeInLegend, minSize))
}

object PieChartWithTooltipInternal {
  import PieChartWithTooltip._

  class Backend[N,V](scope: BackendScope[Props[N,V], Unit]) {
    def render( props: Props[N,V] ) = {
      val (slices,colors) = props.histogram.flatMap { entry => entry._2.map { entry =>
        val (name, value) = entry
        ( props.util.valueToDouble(value), props.util.colorMap(name) )
      }}.unzip
      Tooltip(
        data = TagMod(
            PieChart( props.size, slices, colors ),
            ^.minHeight := props.minSize.px
        ),
        tooltipbody = TagMod(
            <.div(PieChart( props.sizeInLegend, slices, colors )),
            <.div(
              props.util.legend(props.histogram, props.legendtitle)
            )
        ),
        tooltiptitle = props.title
      )
    }

  }

  val component = ScalaComponent.builder[Props[Any,Any]]("PieChartWithTooltip")
                            .stateless
                            .backend(new Backend[Any,Any](_))
                            .renderBackend
                            .build
}

