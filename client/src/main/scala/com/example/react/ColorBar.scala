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
                    addWhite: Boolean )

  /**
   * ColorBar with white in the middle, and hue1 on left and hue2 on right, with dark on the outside.
   */
  def apply( hue1: Double, minLightness1: Double, hue2: Double, minLightness2: Double, n: Int ) =
    component(Props(hue1,minLightness1,n,true,hue2,minLightness2,n,false,true))

  def apply( hue1: Double, minLightness1: Double, n1: Int, darkToLight1: Boolean,
             hue2: Double, minLightness2: Double, n2: Int, darkToLight2: Boolean,
             addWhite: Boolean ) =
    component( Props(hue1,minLightness1,n1,darkToLight1,hue2,minLightness2,n2,darkToLight2,addWhite ) )

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
}

object ColorBarInternal {
  import ColorBar._
  import Utils._

  val titleAttr    = VdomAttr("data-title")

  private def box( color: Color ): TagMod = {
    <.div(
      ^.flex := "0 0 auto",
      ^.width := "20px",
      ^.height := "20px",
      ^.background := s"${color.toHex}"
    )
  }

  private def box( hue: Double, sat: Double, lightness: Double ): TagMod = {
    box( HSLColor( hue, sat, lightness ) )
  }

  private def bar( hue: Double, minLightness: Double, n: Int, darkToLight: Boolean ) = {
    val cols = colors(hue,minLightness,n)
    val c = if (darkToLight) cols else cols.reverse
    c.map( cc => box(cc) ).toTagMod
  }

  val component = ScalaComponent.builder[Props]("ColorBar")
                            .stateless
                            .noBackend
                            .render_P( props => {
                              import BaseStyles._

                              <.div(
                                ^.display := "flex",
                                ^.flexDirection := "row",
                                ^.flexWrap := "nowrap",
                                ^.justifyContent := "center",
                                ^.border := "none",
                                bar( props.hue1, props.minLightness1, props.n1, props.darkToLight1 ),
                                props.addWhite ?= box( props.hue1, 1.0, 1.0 ),
                                bar( props.hue2, props.minLightness2, props.n2, props.darkToLight2 )
                              )
                            })
                            .build
}
