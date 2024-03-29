package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.svg_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.color.Color

/**
  * A PieChartDonut component.
  *
  * This generates a pie chart with a hole in the middle.
  *
  * The slices are sized proportional to their size.
  * The slices are colored and may have titles.
  *
  * To use, just code the following:
  *
  * {{{
  * import com.github.thebridsk.color.Color
  *
  * PieChartDonut(
  *   slices = List(1,1,1),
  *   colors = List(Color("red"), Color("green"), Color("blue"))
  *   size = 24
  * )
  * }}}
  *
  * @author werewolf
  */
object PieChartDonut {

  import ReactColor._

  /**
    * the slices, colors, and sliceTitles lists must be the same size.
    * If both chartTitle and sliceTitles is specified, then sliceTitles is used.
    *
    * @param size the diameter of the circle in px.
    * @param slices the slices to carve up the circle.  Each entry is the relative size of the slice.
    * @param colors the colors of the slices, must be the same size array as slices.
    * @param chartTitle the title of the chart, will be a flyover.  Ignored if sliceTitles is specified.
    * @param sliceTitles the title of slices, will be flyover.
    */
  case class Props(
      size: Double,
      slices: List[Double],
      colors: List[Color],
      chartTitle: Option[String] = None,
      sliceTitles: Option[List[String]] = None
  )

  /**
    * A Donut chart.  The full circle will be used for the chart.
    *
    * Slices, colors, and sliceTitles, if specified must all be the same size.
    * If both chartTitle and sliceTitles is specified, then sliceTitles is used.
    *
    * @param size the diameter of the circle in px.
    * @param slices the slices to carve up the circle.  Each entry is the relative size of the slice.
    * @param colors the colors of the slices, must be the same size array as slices.
    * @param chartTitle the title of the chart, will be a flyover.  Ignored if sliceTitles is specified.
    * @param sliceTitles the title of slices, will be flyover.
    *
    * @return the unmounted react component.
    *
    * @see See [[PieChart]] for usage.
    */
  def apply(
      size: Double,
      slices: List[Double],
      colors: List[Color],
      chartTitle: Option[String] = None,
      sliceTitles: Option[List[String]] = None
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(Props(size, slices, colors, chartTitle, sliceTitles))
  }

// <svg width="100%" height="100%" viewBox="0 0 42 42" class="donut">
//   <circle class="donut-hole" cx="21" cy="21" r="15.91549430918954" fill="#fff"></circle>
//   <circle class="donut-ring" cx="21" cy="21" r="15.91549430918954" fill="transparent" stroke="#d2d3d4" stroke-width="3"></circle>
//
//   <circle class="donut-segment" cx="21" cy="21" r="15.91549430918954" fill="transparent" stroke="#ce4b99" stroke-width="3" stroke-dasharray="40 60" stroke-dashoffset="25"></circle>
//   <circle class="donut-segment" cx="21" cy="21" r="15.91549430918954" fill="transparent" stroke="#b1c94e" stroke-width="3" stroke-dasharray="20 80" stroke-dashoffset="85"></circle>
//   <circle class="donut-segment" cx="21" cy="21" r="15.91549430918954" fill="transparent" stroke="#377bbc" stroke-width="3" stroke-dasharray="30 70" stroke-dashoffset="65"></circle>
//   <!-- unused 10% -->
// </svg>

  private val component =
    ScalaComponent
      .builder[Props]("PieChartDonut")
      .stateless
      .noBackend
      .render_P { props =>
        val sum = props.slices.foldLeft(0.0)(_ + _) / 100
        val chartTitle =
          props.sliceTitles.flatMap(l => None).getOrElse(props.chartTitle)
        <.svg(
          chartTitle.whenDefined(t => <.title(t)),
          ^.width := f"${props.size}%.2f",
          ^.height := f"${props.size}%.2f",
          ^.viewBox := "0 0 50 50",
          props.slices
            .zip(props.colors)
            .zipWithIndex
            .map {
              case ((slice, color), i) =>
                val offset =
                  125 - props.slices.take(i).foldLeft(0.0)(_ + _) / sum
                val stroke = slice / sum
                val title = props.sliceTitles
                  .flatMap(l => if (l.isDefinedAt(i)) Some(l(i)) else None)
                <.circle(
                  title.whenDefined(t => <.title(t)),
                  ^.cx := "25",
                  ^.cy := "25",
                  ^.r := "15.915494309189533576888376337251",
                  ^.fill := "transparent",
                  ^.stroke := color,
                  ^.strokeWidth := "15",
                  ^.strokeDasharray := f"${stroke}%.2f ${100 - stroke}%.2f",
                  ^.strokeDashoffset := f"${offset}%.2f"
                )
            }
            .toTagMod
        )
      }
      .build

}
