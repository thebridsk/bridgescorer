package com.github.thebridsk.bridge.react

import scala.scalajs.js
import japgolly.scalajs.react.vdom.svg_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.bridge.color.Color
import com.github.thebridsk.bridge.pages.BaseStyles
import Utils._

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * PieChart( PieChart.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object PieChart {

  /**
   * @param size the diameter of the circle
   * @param slices the slices to carve up the circle.
   * @param colors the colors of the slices, must be the same size array as slices.
   * @param chartTitle the title of the chart, will be a flyover.  Ignored if sliceTitles is specified.
   * @param sliceTitles the title of slices, will be flyover.
   * Must be the same size array as slices.
   * If both chartTitle and sliceTitles is specified, then sliceTitles is used.
   * Must be the same size array as slices.
   */
  case class Props(
      size: Double,
      slices: List[Double],
      colors: List[Color],
      chartTitle: Option[String] = None,
      sliceTitles: Option[List[String]] = None,
      attrs: Option[TagMod] = None
  )


  /**
   * A pie chart.  The full circle will be used for the chart.
   * @param size the relative area of the circle
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
  ) = {
    component(Props(size,slices,colors,chartTitle,sliceTitles,attrs))
  }

  def apply( props: Props ) = component(props)

  private def getCoordinatesForPercent( fraction: Double ) = {
    val x = Math.cos(2 * Math.PI * fraction);
    val y = Math.sin(2 * Math.PI * fraction);

    (x, y)
  }

  private val component =
    ScalaComponent.builder[Props]("PieChart")
      .stateless
      .noBackend
      .render_P { props =>
        val chartTitle = props.sliceTitles.flatMap( l => None ).getOrElse( props.chartTitle )
        val slices = props.slices.zip( props.colors ).filter( e => e._1 != 0 )
        if (slices.length == 1) {
          <.svg(
            chartTitle.whenDefined( t => <.title(t) ),
            ^.width := f"${props.size}%.2f",
            ^.height := f"${props.size}%.2f",
            ^.viewBox := "-1.1 -1.1 2.2 2.2",
            BaseStyles.baseStyles.piechart,
            <.circle(
              props.sliceTitles.flatMap { list =>
                props.slices.zip( list ).find( e => e._1 != 0 ).
                  map( t => Some(<.title(t._2)) ).getOrElse(None)
              }.whenDefined,
              ^.cx := 0,
              ^.cy := 0,
              ^.r := 1,
              ^.fill := slices.head._2
            ),
            props.attrs.whenDefined
          )
        } else {
          val sum = props.slices.foldLeft(0.0)( _ + _ )
          var sofar = 0.0
          val (x,y) = getCoordinatesForPercent(sofar)
          var lastx = x
          var lasty = y
          <.svg(
            chartTitle.whenDefined( t => <.title(t) ),
            ^.width := f"${props.size}%.2f",
            ^.height := f"${props.size}%.2f",
            ^.viewBox := "-1.1 -1.1 2.2 2.2",
            BaseStyles.baseStyles.piechart,
            props.slices.zip( props.colors ).zipWithIndex.flatMap { case ( (slice, color), i ) =>
              if (slice == 0) None
              else {
                val sweep = slice/sum
                val largeArcFlag = if (sweep >= 0.5) 1 else 0
                sofar = sofar + slice/sum
                val sourcex = lastx
                val sourcey = lasty
                val (targetx,targety) = getCoordinatesForPercent(sofar)
                lastx = targetx
                lasty = targety
                val title = props.sliceTitles.flatMap( l => if (l.isDefinedAt(i)) Some(l(i)) else None )
                Some( <.path(
                  title.whenDefined( t => <.title(t) ),
                  ^.d := f"M ${sourcex}%.2f ${sourcey}%.2f" +
                         f" A 1 1 0 ${largeArcFlag} 1 ${targetx}%.2f ${targety}%.2f" +
                          " L 0 0",
                  ^.fill := color,
                ))
              }
            }.toTagMod,
            props.attrs.whenDefined
          )
        }
      }.build

}

