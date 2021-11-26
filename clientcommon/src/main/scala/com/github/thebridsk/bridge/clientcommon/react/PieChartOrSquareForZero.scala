package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.svg_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.react.PieChart.Props

/**
  * A component that displays as a pie chart or a square.
  *
  * The square is used if the size is less than zero.
  *
  * The slices are sized proportional to their size.
  * The slices are colored and may have titles.
  *
  * To use, just code the following:
  *
  * {{{
  * import com.github.thebridsk.color.Color
  *
  * PieChart(
  *   slices = List(1,1,1),
  *   colors = List(Color("red"), Color("green"), Color("blue"))
  *   size = 24
  * )
  * }}}
  *
  * @author werewolf
  */
object PieChartOrSquareForZero {

  /**
    * The Properties
    *
    * @param piechartProps
    */
  case class SquareProps(
      piechartProps: Props
  )

  /**
    * A pie chart.  The full circle will be used for the chart.
    * If the size less than zero, then a square of size -size will be drawn.
    *
    * Slices, colors, and sliceTitles, if specified must all be the same size.
    * If both chartTitle and sliceTitles is specified, then sliceTitles is used.
    *
    * @param size if greater than zero then it is the diameter of the circle in px.
    *             if less than zero then it is the negative size of the square in px.
    * @param slices the slices to carve up the circle.  Each entry is the relative size of the slice.
    * @param colors the colors of the slices, must be the same size array as slices.
    * @param chartTitle the title of the chart, will be a flyover.  Ignored if sliceTitles is specified.
    * @param sliceTitles the title of slices, will be flyover.
    * @param attrs attributes to add to the <svg> element
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
      sliceTitles: Option[List[String]] = None,
      attrs: Option[TagMod] = None
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      SquareProps(
        Props(slices, Some(colors), chartTitle, sliceTitles, Some(size), attrs)
      )
    )
  }

  private def getCoordinatesForPercent(fraction: Double) = {
    val x = Math.cos(2 * Math.PI * fraction);
    val y = Math.sin(2 * Math.PI * fraction);

    (x, y)
  }

  private val component =
    ScalaComponent
      .builder[SquareProps]("PieChartOrSquareForZero")
      .stateless
      .noBackend
      .render_P { props =>
        if (props.piechartProps.size.get < 0) {
          val chartTitle = props.piechartProps.sliceTitles
            .flatMap(l => None)
            .getOrElse(props.piechartProps.chartTitle)
          <.svg(
            chartTitle.whenDefined(t => <.title(t)),
            ^.width := f"${-props.piechartProps.size.get}%.2f",
            ^.height := f"${-props.piechartProps.size.get}%.2f",
            ^.viewBox := "-10.1 -10.1 20.2 20.2",
            BaseStyles.baseStyles.piechartzero,
            <.rect(
              ^.x := -10,
              ^.y := -10,
              ^.width := 20,
              ^.height := 20,
              ^.strokeWidth := 5,
              ^.fill := "transparent"
            ),
            props.piechartProps.attrs.whenDefined
          )
        } else {
          PieChart(props.piechartProps)
        }
      }
      .build

}
