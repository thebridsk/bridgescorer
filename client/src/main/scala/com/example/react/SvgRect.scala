package com.example.react

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.svg_<^._
import japgolly.scalajs.react._
import org.scalajs.dom.ext.Color
import com.example.pages.BaseStyles
import Utils._

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * SvgRect( SvgRect.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object SvgRect {

  /**
   * Makes a rectangle where the slices are vertical.
   * @param height the height of the rectangle
   * @param width the width of the rectangle
   * @param borderColor
   * @param slices the slices to carve up the circle.
   * @param colors the colors of the slices, must be the same size array as slices.
   * @param chartTitle the title of the chart, will be a flyover.  Ignored if sliceTitles is specified.
   * @param sliceTitles the title of slices, will be flyover.
   * Must be the same size array as slices.
   * If both chartTitle and sliceTitles is specified, then sliceTitles is used.
   * Must be the same size array as slices.
   */
  case class Props(
      height: Double,
      width: Double,
      borderColor: Color,
      slices: List[Double],
      colors: List[Color],
      chartTitle: Option[String] = None,
      sliceTitles: Option[List[String]] = None,
      attrs: Option[TagMod] = None
  )


  /**
   * Makes a rectangle where the slices are vertical.
   * @param height the height of the rectangle
   * @param width the width of the rectangle
   * @param borderColor
   * @param slices the slices to carve up the circle.  The sum of the entries MUST NOT be 0
   * @param colors the colors of the slices, must be the same size array as slices.
   * @param chartTitle the title of the chart, will be a flyover.  Ignored if sliceTitles is specified.
   * @param sliceTitles the title of slices, will be flyover.
   * Must be the same size array as slices.
   * If both chartTitle and sliceTitles is specified, then sliceTitles is used.
   * Must be the same size array as slices.
   */
  def apply(
      height: Double,
      width: Double,
      borderColor: Color,
      slices: List[Double],
      colors: List[Color],
      chartTitle: Option[String] = None,
      sliceTitles: Option[List[String]] = None,
      attrs: Option[TagMod] = None
  ) = {
    component(Props(height,width,borderColor,slices,colors,chartTitle,sliceTitles,attrs))
  }

  def apply( props: Props ) = component(props)

  private val component =
    ScalaComponent.builder[Props]("SvgRect")
      .stateless
      .noBackend
      .render_P { props =>
        val chartTitle = props.sliceTitles.flatMap( l => None ).getOrElse( props.chartTitle )
        val slices = props.slices.zip( props.colors ).filter( e => e._1 != 0 )
        val sum = props.slices.foldLeft(0.0)( _ + _ )
        var sofar = 0.0
        val x = sofar
        var lastx = x
        <.svg(
          chartTitle.whenDefined( t => <.title(t) ),
          ^.width := f"${props.width}%.2f",
          ^.height := f"${props.height}%.2f",
          ^.viewBox := f"-0.1 -0.1 ${props.width+0.2}%.2f ${props.height+0.2}%.2f",
          BaseStyles.baseStyles.svgrect,
          props.slices.zip( props.colors ).zipWithIndex.flatMap { case ( (slice, color), i ) =>
            if (slice == 0) None
            else {
              val sweep = slice/sum
              sofar = sofar + slice/sum
              val sourcex = lastx
              val targetx = sofar
              lastx = targetx
              val title = props.sliceTitles.flatMap( l => if (l.isDefinedAt(i)) Some(l(i)) else None )
              Some( <.rect(
                title.whenDefined( t => <.title(t) ),
                ^.x := s"${sourcex*props.width}",
                ^.width := s"${(targetx-sourcex)*props.width}",
                ^.y := s"0",
                ^.height := s"${props.height}",
                ^.stroke := props.borderColor,
                ^.strokeWidth := 0.5,
                ^.fill := color,
              ))
            }
          }.toTagMod,
          props.attrs.whenDefined
        )
      }.build

}

