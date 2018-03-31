package com.example.react

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import com.example.pages.BaseStyles
import japgolly.scalajs.react.vdom.HtmlStyles
import org.scalajs.dom.ext.Color

/**
 * A skeleton component.
 *
 * To use, just code the following:
 *
 * <pre><code>
 * AppButton( AppButton.Props( ... ) )
 * </code></pre>
 *
 * @author werewolf
 */
object ColorBar {
  import ColorBarInternal._

  case class Props( hue1: Double, minLightness1: Double, n1: Int, darkToLight1: Boolean,
                    hue2: Double, minLightness2: Double, n2: Int, darkToLight2: Boolean,
                    middle: Option[Color],
                    titles1: Option[List[String]] = None,
                    titles2: Option[List[String]] = None,
                    whiteTitle: Option[String] = None
                    )

  /**
   * ColorBar with white in the middle, and hue1 on left and hue2 on right, with dark on the outside.
   * No flyover text on the color bar.
   * @param hue1
   * @param minLightness1
   * @param hue2
   * @param minLightness2
   * @param n the number of boxes for hue1 and hue2
   */
  def apply( hue1: Double, minLightness1: Double, hue2: Double, minLightness2: Double, n: Int ) = {
    component(Props(hue1,minLightness1,n,true,hue2,minLightness2,n,false,Some(Color.White), None, None, None))
  }

  /**
   * ColorBar with white in the middle, and hue1 on left and hue2 on right, with dark on the outside.
   * Titles will show as flyover text.  No title for the middle white box.
   * @param hue1
   * @param minLightness1
   * @param hue2
   * @param minLightness2
   * @param n the number of boxes for hue1 and hue2
   * @param titles1 the titles of the left boxes, must have n titles.
   * @param titles2 the titles of the right boxes, must have n titles.
   */
  def apply( hue1: Double, minLightness1: Double, hue2: Double, minLightness2: Double, n: Int,
             titles1: List[String],
             titles2: List[String]
  ) = {
    component(Props(hue1,minLightness1,n,true,hue2,minLightness2,n,false,Some(Color.White), Option(titles1), Option(titles2), None))
  }

  /**
   * ColorBar with white in the middle, and hue1 on left and hue2 on right, with dark on the outside.
   * Titles will show as flyover text.
   * @param hue1
   * @param minLightness1
   * @param hue2
   * @param minLightness2
   * @param n the number of boxes for hue1 and hue2
   * @param titles1 the titles of the left boxes, must have n titles.
   * @param titles2 the titles of the right boxes, must have n titles.
   * @param whiteTitle
   */
  def apply( hue1: Double, minLightness1: Double, hue2: Double, minLightness2: Double, n: Int,
             titles1: List[String],
             titles2: List[String],
             whiteTitle: String
  ) = {
    component(Props(hue1,minLightness1,n,true,hue2,minLightness2,n,false,Some(Color.White), Option(titles1), Option(titles2), Option(whiteTitle)))
  }

  /**
   * ColorBar with hue1 on the left, optional white in the middle, and hue2 on the right.
   * Optional titles can be specified, and will show as flyover text.
   * @param hue1
   * @param minLightness1
   * @param n1 the number of boxes for hue1
   * @param darkToLight1 left boxes should be dark to light if true.
   * @param hue2
   * @param minLightness2
   * @param n2 the number of boxes for hue2
   * @param darkToLight2 right boxes should be dark to light if true.
   * @param middle the optional middle color
   * @param titles1 the titles of the left boxes, if specified, must have n1 titles.
   * @param titles2 the titles of the right boxes, if specified, must have n2 titles.
   * @param whiteTitle the title of the white box.
   */
  def apply( hue1: Double, minLightness1: Double, n1: Int, darkToLight1: Boolean,
             hue2: Double, minLightness2: Double, n2: Int, darkToLight2: Boolean,
             middle: Option[Color],
             titles1: Option[List[String]] = None,
             titles2: Option[List[String]] = None,
             whiteTitle: Option[String] = None
  ) = {
    component( Props(hue1,minLightness1,n1,darkToLight1,hue2,minLightness2,n2,darkToLight2,middle, titles1, titles2, whiteTitle ) )
  }

  /**
   * returns the colors from dark to light.  never returns white.
   */
  def colors( hue: Double, minLightness: Double, n: Int ) = {
    if (n == 0) Nil
    else if (n == 1) HSLColor( hue, 1.0, 0.5 )::Nil
    else {
      val step = (1.0 - minLightness)/(n)
      (minLightness until 1.0 by step).map { l =>
         HSLColor( hue, 1.0, l )
      }
    }
  }

  case class PropsColors(
      leftColors: Seq[Color],
      rightColors: Seq[Color],
      middle: Option[Color],
      leftTitles: Option[List[String]] = None,
      rightTitles: Option[List[String]] = None,
      whiteTitle: Option[String] = None
  )

  /**
   * ColorBar with hue1 on the left, optional white in the middle, and hue2 on the right.
   * Optional titles can be specified, and will show as flyover text.
   * @param leftColors list of colors for left of middle
   * @param rightColors list of colors for right of middle
   * @param middle the optional middle color
   * @param leftTitles the titles of the left boxes, if specified, must have leftColors.length titles.
   * @param rightTitles the titles of the right boxes, if specified, must have rightColors.length titles.
   * @param whiteTitle the title of the white box.
   */
  def create(
      leftColors: Seq[Color],
      rightColors: Seq[Color],
      middle: Option[Color],
      leftTitles: Option[List[String]] = None,
      rightTitles: Option[List[String]] = None,
      whiteTitle: Option[String] = None
  ) = {
    componentColors( PropsColors( leftColors, rightColors, middle, leftTitles, rightTitles, whiteTitle ) )
  }

  /**
   * Simple ColorBar
   * @param colors
   * @param titles
   */
  def simple(
      colors: List[Color],
      titles: Option[List[String]] = None
  ) = {
    componentColors(PropsColors(colors, Nil, None, titles, None, None))
  }

}

object ColorBarInternal {
  import ColorBar._
  import Utils._

  import BaseStyles._

  def box( color: Color, title: Option[String] ): VdomNode = {
    val d: VdomNode = <.div(
      ^.flex := "0 0 auto",
      ^.width := "20px",
      ^.height := "20px",
      ^.background := s"${color.toHex}"
    )
    title.map { t =>
      val tt: VdomNode = Tooltip( d, t )
      tt
    }.getOrElse(d)
  }

  def box( hue: Double, sat: Double, lightness: Double, title: Option[String] ): TagMod = {
    box( HSLColor( hue, sat, lightness ), title )
  }

  def bar( hue: Double, minLightness: Double, n: Int, darkToLight: Boolean, titles: Option[List[String]] ): TagMod = {
    val cols = colors(hue,minLightness,n)
    val c = if (darkToLight) cols else cols.reverse
    bar(c, titles)
  }

  def bar( cols: Seq[Color], titles: Option[List[String]] ): TagMod = {
    val ts = titles.map( t => t.map( ti => Some(ti) ) ).getOrElse( cols.map( cols => None ) )
    cols.zip( ts ).map { entry =>
      val (cc,t) = entry
      box(cc,t)
    }.toTagMod
  }

  val component = ScalaComponent.builder[Props]("ColorBar")
                            .stateless
                            .noBackend
                            .render_P( props => {

                              <.div(
                                baseStyles.colorbar,
                                ^.display := "flex",
                                ^.flexDirection := "row",
                                ^.flexWrap := "nowrap",
                                ^.justifyContent := "center",
                                ^.border := "none",
                                bar( props.hue1, props.minLightness1, props.n1, props.darkToLight1, props.titles1 ),
                                props.middle.whenDefined( c => box( c, props.whiteTitle )),
                                bar( props.hue2, props.minLightness2, props.n2, props.darkToLight2, props.titles2 )
                              )
                            })
                            .build

  val componentColors = ScalaComponent.builder[PropsColors]("ColorBarColors")
                            .stateless
                            .noBackend
                            .render_P( props => {

                              <.div(
                                baseStyles.colorbar,
                                ^.display := "flex",
                                ^.flexDirection := "row",
                                ^.flexWrap := "nowrap",
                                ^.justifyContent := "center",
                                ^.border := "none",
                                bar( props.leftColors, props.leftTitles ),
                                props.middle.whenDefined( c => box( c, props.whiteTitle )),
                                bar( props.rightColors, props.rightTitles )
                              )
                            })
                            .build
}
