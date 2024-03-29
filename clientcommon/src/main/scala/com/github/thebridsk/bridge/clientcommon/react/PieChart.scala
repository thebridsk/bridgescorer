package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.svg_<^._
import japgolly.scalajs.react._
import com.github.thebridsk.color.Color
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles

/**
  * A PieChart component.
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
object PieChart {

  import ReactColor._

  /**
    * Properties for PieChart component.
    *
    * Slices, colors, sliceTitles, and sliceAttrs, if specified must all be the same size.
    * If both chartTitle and sliceTitles is specified, then sliceTitles is used.
    *
    * @param slices the slices to carve up the circle.  Each entry is the relative size of the slice.
    * @param colors the colors of the slices, must be the same size array as slices.
    * @param chartTitle the title of the chart, will be a flyover.  Ignored if sliceTitles is specified.
    * @param sliceTitles the title of slices, will be flyover.
    * @param size the diameter of the circle in px.
    * @param attrs attributes to add to the <svg> element
    * @param sliceAttrs attributes to add to each slice's <path> element.
    */
  case class Props(
      slices: List[Double],
      colors: Option[List[Color]],
      chartTitle: Option[String] = None,
      sliceTitles: Option[List[String]] = None,
      size: Option[Double] = None,
      attrs: Option[TagMod] = None,
      sliceAttrs: Option[List[TagMod]] = None
  )

  /**
    * A pie chart.  The full circle will be used for the chart.
    *
    * Slices, colors, sliceTitles, and sliceAttrs, if specified must all be the same size.
    * If both chartTitle and sliceTitles is specified, then sliceTitles is used.
    *
    * @param slices the slices to carve up the circle.  Each entry is the relative size of the slice.
    * @param colors the colors of the slices, must be the same size array as slices.
    * @param chartTitle the title of the chart, will be a flyover.  Ignored if sliceTitles is specified.
    * @param sliceTitles the title of slices, will be flyover.
    * @param size the diameter of the circle in px.
    * @param attrs attributes to add to the <svg> element
    * @param sliceAttrs attributes to add to each slice's <path> element.
    *
    * @return the unmounted react component.
    *
    * @see See [[PieChart]] for usage.
    */
  def create(
      slices: List[Double],
      colors: Option[List[Color]],
      chartTitle: Option[String] = None,
      sliceTitles: Option[List[String]] = None,
      size: Option[Double] = None,
      attrs: Option[TagMod] = None,
      sliceAttrs: Option[List[TagMod]] = None
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props(slices, colors, chartTitle, sliceTitles, size, attrs, sliceAttrs)
    )
  }

  /**
    * A pie chart.  The full circle will be used for the chart.
    *
    * Slices, colors, sliceTitles, and sliceAttrs, if specified must all be the same size.
    * If both chartTitle and sliceTitles is specified, then sliceTitles is used.
    *
    * @param size the diameter of the circle in px.
    * @param slices the slices to carve up the circle.  Each entry is the relative size of the slice.
    * @param colors the colors of the slices, must be the same size array as slices.
    * @param chartTitle the title of the chart, will be a flyover.  Ignored if sliceTitles is specified.
    * @param sliceTitles the title of slices, will be flyover.
    * @param attrs attributes to add to the <svg> element
    * @param sliceAttrs attributes to add to each slice's <path> element.
    *
    * @return the unmounted react component.
    *
    * @see See [[PieChart]] for usage.
    *
    */
  def apply(
      size: Double,
      slices: List[Double],
      colors: Option[List[Color]],
      chartTitle: Option[String] = None,
      sliceTitles: Option[List[String]] = None,
      attrs: Option[TagMod] = None,
      sliceAttrs: Option[List[TagMod]] = None
  ) = { // scalafix:ok ExplicitResultTypes; ReactComponent
    component(
      Props(
        slices,
        colors,
        chartTitle,
        sliceTitles,
        Some(size),
        attrs,
        sliceAttrs
      )
    )
  }

  /**
    * A pie chart.  The full circle will be used for the chart.
    *
    * @param props
    * @return the unmounted react component.
    *
    * @see See [[PieChart]] for usage.
    */
  def apply(props: Props) =
    component(props) // scalafix:ok ExplicitResultTypes; ReactComponent

  private def getCoordinatesForPercent(fraction: Double) = {
    val x = Math.cos(2 * Math.PI * fraction);
    val y = Math.sin(2 * Math.PI * fraction);

    (x, y)
  }

  private val component =
    ScalaComponent
      .builder[Props]("PieChart")
      .stateless
      .noBackend
      .render_P { props =>
        val chartTitle =
          props.sliceTitles.flatMap(l => None).getOrElse(props.chartTitle)
        val colors = props.colors
          .map { cs =>
            cs.map { c =>
              val r = ^.fill := c; r
            }
          }
          .getOrElse((0 until props.slices.length).map(n => TagMod()))
        val sliceattr = props.sliceAttrs.getOrElse(
          (0 until props.slices.length).map(n => TagMod())
        )
        val slicetitles = props.sliceTitles
          .map { cs => cs.map { t => Some(t) } }
          .getOrElse((0 until props.slices.length).map(n => None))
        val slices = props.slices
          .zip(colors)
          .zip(sliceattr)
          .zip(slicetitles)
          .filter(e => e._1._1._1 != 0)
        if (slices.length == 1) {
          val (((slice, color), attr), slicetitle) = slices.head
          <.svg(
            chartTitle.whenDefined(t => <.title(t)),
            props.size.whenDefined { s =>
              TagMod(
                ^.width := f"${s}%.2f",
                ^.height := f"${s}%.2f"
              )
            },
            ^.viewBox := "-1.1 -1.1 2.2 2.2",
            BaseStyles.baseStyles.piechart,
            <.circle(
              slicetitle.whenDefined(t => <.title(t)),
              attr,
              ^.cx := 0,
              ^.cy := 0,
              ^.r := 1,
              color
            ),
            props.attrs.whenDefined
          )
        } else {
          val sum = props.slices.foldLeft(0.0)(_ + _)
          var sofar = 0.0
          val (x, y) = getCoordinatesForPercent(sofar)
          var lastx = x
          var lasty = y
          <.svg(
            chartTitle.whenDefined(t => <.title(t)),
            props.size.whenDefined { s =>
              TagMod(
                ^.width := f"${s}%.2f",
                ^.height := f"${s}%.2f"
              )
            },
            ^.viewBox := "-1.1 -1.1 2.2 2.2",
            BaseStyles.baseStyles.piechart,
            slices.map { e =>
              val (((slice, color), attr), slicetitle) = e
              if (slice == 0) None
              else {
                val sweep = slice / sum
                val largeArcFlag = if (sweep >= 0.5) 1 else 0
                sofar = sofar + slice / sum
                val sourcex = lastx
                val sourcey = lasty
                val (targetx, targety) = getCoordinatesForPercent(sofar)
                lastx = targetx
                lasty = targety
                Some(
                  <.path(
                    slicetitle.whenDefined(t => <.title(t)),
                    ^.d := f"M ${sourcex}%.2f ${sourcey}%.2f" +
                      f" A 1 1 0 ${largeArcFlag} 1 ${targetx}%.2f ${targety}%.2f" +
                      " L 0 0",
                    color,
                    attr
                  )
                )
              }
            }.toTagMod,
            props.attrs.whenDefined
          )
        }
      }
      .build

}
