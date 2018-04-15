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

  /**
   * Props for ColorBar
   * @constructor
   * @param leftColors list of colors for left, may be empty list
   * @param rightColors list of colors for right, may be empty list
   * @param middle the optional middle color
   * @param leftTitles the titles of the left boxes, if specified, must have leftColors.length titles.  None means no titles.
   * @param rightTitles the titles of the right boxes, if specified, must have rightColors.length titles.  None means no titles.
   * @param whiteTitle the title of the white box.  None means no title.
   */
  case class Props(
      leftColors: Seq[Color],
      rightColors: Seq[Color],
      middle: Option[Color],
      leftTitles: Option[List[String]] = None,
      rightTitles: Option[List[String]] = None,
      whiteTitle: Option[String] = None
  ) {
  }

  object Props {
    /**
     * Create a Props object.
     * The first four parameters are for the left color boxes,
     * the second four are for the right color boxes.
     * @param hue1
     * @param minLightness1
     * @param n1 the number of boxes for hue1
     * @param darkToLight1 left boxes should be dark to light if true.
     * @param hue2
     * @param minLightness2
     * @param n2 the number of boxes for hue2
     * @param darkToLight2 right boxes should be dark to light if true.
     * @param middle the optional middle color
     * @param titles1 the titles of the left boxes, if specified, must have leftColors.length titles.  None means no titles.
     * @param titles2 the titles of the right boxes, if specified, must have rightColors.length titles.  None means no titles.
     * @param whiteTitle the title of the white box.  None means no title.
     */
    def create(
        hue1: Double, minLightness1: Double, n1: Int, darkToLight1: Boolean,
        hue2: Double, minLightness2: Double, n2: Int, darkToLight2: Boolean,
        middle: Option[Color],
        titles1: Option[List[String]] = None,
        titles2: Option[List[String]] = None,
        whiteTitle: Option[String] = None
    ) = {
      new Props(
          colors(hue1,minLightness1,n1,darkToLight1),
          colors(hue2,minLightness2,n2,darkToLight2),
          middle,
          titles1,
          titles2,
          whiteTitle
      )
    }
  }

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
    component(Props.create(hue1,minLightness1,n,true,hue2,minLightness2,n,false,Some(Color.White), None, None, None))
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
    component(Props.create(hue1,minLightness1,n,true,hue2,minLightness2,n,false,Some(Color.White), Option(titles1), Option(titles2), None))
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
    component(Props.create(hue1,minLightness1,n,true,hue2,minLightness2,n,false,Some(Color.White), Option(titles1), Option(titles2), Option(whiteTitle)))
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
    component( Props.create(hue1,minLightness1,n1,darkToLight1,hue2,minLightness2,n2,darkToLight2,middle, titles1, titles2, whiteTitle ) )
  }

  /**
   * returns the colors
   * @param hue
   * @param minLightness
   * @param n the number of boxes for hue
   * @param darkToLight left boxes should be dark to light if true.  Default is true.
   * @param maxLightness
   * @return the colors.
   */
  def colors( hue: Double, minLightness: Double, n: Int, darkToLight1: Boolean = true, maxLightness: Double = 1.0 ) = {
    if (n == 0) Nil
    else if (n == 1) HSLColor( hue, 1.0, 0.5 )::Nil
    else {
      val step = (maxLightness - minLightness)/(n)
      val cols = (minLightness until maxLightness by step).map { l =>
         HSLColor( hue, 1.0, l )
      }
      if (darkToLight1) cols
      else cols.reverse
    }
  }

  /**
   * returns the colors
   * @param hue
   * @param minLightness
   * @param n the number of boxes for hue
   * @param darkToLight left boxes should be dark to light if true.  Default is true.
   * @param maxLightness
   * @return the colors.
   */
  def colorsInclusive( hue: Double, minLightness: Double, n: Int, darkToLight1: Boolean = true, maxLightness: Double = 1.0 ) = {
    if (n == 0) Nil
    else if (n == 1) HSLColor( hue, 1.0, 0.5 )::Nil
    else {
      val step = (maxLightness - minLightness)/(n-1)
      val cols = (minLightness to maxLightness by step).map { l =>
         HSLColor( hue, 1.0, l )
      }
      if (darkToLight1) cols
      else cols.reverse
    }
  }

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
    component( Props( leftColors, rightColors, middle, leftTitles, rightTitles, whiteTitle ) )
  }

  /**
   * Simple ColorBar
   * @param colors
   * @param titles
   */
  def simple(
      colors: Seq[Color],
      titles: Option[List[String]] = None
  ) = {
    component(Props(colors, Nil, None, titles, None, None))
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
                        bar( props.leftColors, props.leftTitles ),
                        props.middle.whenDefined( c => box( c, props.whiteTitle )),
                        bar( props.rightColors, props.rightTitles )
                      )
                    })
                    .build
}
