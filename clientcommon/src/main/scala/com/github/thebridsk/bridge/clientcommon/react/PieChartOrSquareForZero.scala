package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.svg_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.react.PieChart.Props


/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PieChartOrSquareForZero( PieChartOrSquareForZero.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PieChartOrSquareForZero {


  case class SquareProps(
    piechartProps: Props
  )

  /**
   * A pie chart.  The full circle will be used for the chart.
   * if the size less than zero, then a square of size -size will be drawn.
   * @param size the relative area of the circle.  If negative then a square of size -size is drawn.
   * @param squareColor the color if a square is drawn
   * @param slices the slices to carve up the circle.  The sum of the entries MUST NOT be 0
   * @param colors the colors of the slices, must be the same size array as slices.
   * @param chartTitle the title of the chart, will be a flyover.  Ignored if sliceTitles is specified.
   * @param sliceTitles the title of slices, will be flyover.
   * Must be the same size array as slices.
   * If both chartTitle and sliceTitles is specified, then sliceTitles is used.
   * Must be the same size array as slices.
   */
  def apply(
      size: Double,
      slices: List[Double],
      colors: List[Color],
      chartTitle: Option[String] = None,
      sliceTitles: Option[List[String]] = None,
      attrs: Option[TagMod] = None
  ) = {  // scalafix:ok ExplicitResultTypes; ReactComponent
    component(SquareProps( Props(slices,Some(colors),chartTitle,sliceTitles,Some(size),attrs)))
  }

  private def getCoordinatesForPercent( fraction: Double ) = {
    val x = Math.cos(2 * Math.PI * fraction);
    val y = Math.sin(2 * Math.PI * fraction);

    (x, y)
  }

  private val component =
    ScalaComponent.builder[SquareProps]("PieChartOrSquareForZero")
      .stateless
      .noBackend
      .render_P { props =>
        if (props.piechartProps.size.get < 0) {
          val chartTitle = props.piechartProps.sliceTitles.flatMap( l => None ).getOrElse( props.piechartProps.chartTitle )
          <.svg(
            chartTitle.whenDefined( t => <.title(t) ),
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
          PieChart( props.piechartProps )
        }
      }.build

}

